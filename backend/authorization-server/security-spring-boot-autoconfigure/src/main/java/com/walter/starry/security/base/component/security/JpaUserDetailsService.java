package com.walter.starry.security.base.component.security;

import com.walter.starry.security.base.bo.AclUserBo;
import com.walter.starry.security.base.component.security.oauth2.OidcUserDetailsService;
import com.walter.starry.security.base.entity.AclAuthority;
import com.walter.starry.security.base.entity.AclUser;
import com.walter.starry.security.base.repository.AclAuthorityRepository;
import com.walter.starry.security.base.repository.AclUserRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.*;

/**
 * @author: walter.tan
 * @datetime: 2023/9/12 22:58
 */
public class JpaUserDetailsService extends JdbcUserDetailsManager implements OidcUserDetailsService {

    private static final String DEFAULT_PASSWORD = "123456";

    private AclUserRepository aclUserRepository;

    private AclAuthorityRepository aclAuthorityRepository;

    private FindByIndexNameSessionRepository<? extends Session> findByIndexNameSessionRepository;

    public JpaUserDetailsService(){
    }

    public JpaUserDetailsService(DataSource dataSource, AclUserRepository aclUserRepository, AclAuthorityRepository aclAuthorityRepository, FindByIndexNameSessionRepository<? extends Session> findByIndexNameSessionRepository) {
        super.setDataSource(dataSource);
        this.aclUserRepository = aclUserRepository;
        this.aclAuthorityRepository = aclAuthorityRepository;
        this.findByIndexNameSessionRepository = findByIndexNameSessionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserDetails user) {
        // 生成密码和权限列表
        UserDetails userDetails = User.builder()
                .username(user.getUsername())
                .passwordEncoder(pwd -> "{bcrypt}" + new BCryptPasswordEncoder().encode(pwd))
                .password(DEFAULT_PASSWORD)
                .roles("USER")
                .build();

        AclUserBo aclUserBo = ((AclUserBo) user);
        aclUserBo.setPassword(userDetails.getPassword());
        aclUserBo.setAuthorities(new HashSet<>(userDetails.getAuthorities()));

        // 校验
        validateUserDetails(aclUserBo);

        // 保存用户
        Date now = new Date();
        AclUser aclUser = new AclUser();
        aclUser.setUsername(aclUserBo.getUsername());
        aclUser.setNickname(aclUserBo.getNickname());
        aclUser.setPassword(aclUserBo.getPassword());
        aclUser.setOidcRegistrationId(null);
        aclUser.setOpenId(null);
        aclUser.setEnabled(aclUserBo.isEnabled());
        aclUser.setAccountLocked(!aclUserBo.isAccountNonLocked());
        aclUser.setAccountExpired(!aclUserBo.isAccountNonExpired());
        aclUser.setCredentialsExpired(!aclUserBo.isCredentialsNonExpired());
        aclUser.setExpiredSessionsCleanTime(now);
        aclUser.setCreateTime(now);
        aclUser.setUpdateTime(now);
        aclUserRepository.save(aclUser);

        if (getEnableAuthorities()) {
            // 保存用户权限关系
            List<AclAuthority> aclAuthorityList = aclUserBo.getAuthorities().stream().map(au -> {
                AclAuthority aclAuthority = new AclAuthority();
                aclAuthority.setUsername(user.getUsername());
                aclAuthority.setAuthority(au.getAuthority());
                aclAuthority.setCreateTime(now);
                aclAuthority.setUpdateTime(now);
                return aclAuthority;
            }).toList();

            aclAuthorityRepository.saveAll(aclAuthorityList);
        }
    }

    private void validateUserDetails(AclUserBo user) {
        Assert.hasText(user.getUsername(), "Username may not be empty or null");
        Assert.hasText(user.getNickname(), "Nickname may not be empty or null");
        validateAuthorities(user.getAuthorities());
    }

    private void validateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "Authorities list must not be null");
        for (GrantedAuthority authority : authorities) {
            Assert.notNull(authority, "Authorities list contains a null entry");
            Assert.hasText(authority.getAuthority(), "getAuthority() method must return a non-empty string");
        }
    }

    @Override
    protected List<UserDetails> loadUsersByUsername(String username) {
        AclUser probe = new AclUser();
        probe.setUsername(username);
        Example<? extends AclUser> example = Example.of(probe);
        return aclUserRepository.findAll(example).stream().map(u ->
                (UserDetails) new AclUserBo(u.getUsername(), u.getNickname(), u.getPassword(), u.getOidcRegistrationId(), u.getOpenId(),
                !u.getAccountExpired(), !u.getCredentialsExpired(), !u.getAccountLocked(), u.getEnabled(),
                AuthorityUtils.NO_AUTHORITIES)).toList();
    }

    @Override
    public UserDetails loadUserByRegistrationIdAndOpenId(String registrationId, String openId) throws UsernameNotFoundException {
        AclUser probe = new AclUser();
        probe.setOidcRegistrationId(registrationId);
        probe.setOpenId(openId);
        return aclUserRepository.findOne(Example.of(probe))
                .map(user -> this.loadUserByUsername(user.getUsername()))
                .orElse(null);
    }

    @Override
    protected UserDetails createUserDetails(String username, UserDetails userFromUserQuery, List<GrantedAuthority> combinedAuthorities) {
        AclUserBo user = (AclUserBo) userFromUserQuery;

        String returnUsername = user.getUsername();
        if (!this.isUsernameBasedPrimaryKey()) {
            returnUsername = username;
        }

        return new AclUserBo(returnUsername, user.getNickname(), user.getPassword(), user.getOauth2RegistrationId(), user.getOpenId(),
                user.isAccountNonExpired(), user.isAccountNonLocked(), user.isCredentialsNonExpired(), user.isEnabled(),
                user.getExpiredSessionsCleanTime(), user.getCreateTime(), user.getUpdateTime(), combinedAuthorities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String username) {
        if (getEnableAuthorities()) {
            // 删除用户权限关系
            deleteUserAuthorities(username);
        }

        // 删除数据库
        aclUserRepository.deleteById(username);

        // 删除用户Session
        this.removeSession(username);
    }

    /**
     * 用户角色授权
     * @param username
     * @param newRoleCodeList 待添加的角色列表
     * @param removeRoleCodeList 待删除的角色列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantAuthority(String username, List<String> newRoleCodeList, List<String> removeRoleCodeList){
        Date now = new Date();
        if(CollectionUtils.isNotEmpty(removeRoleCodeList)){
            aclAuthorityRepository.deleteByUsernameAndAuthorityIn(username, removeRoleCodeList);
        }

        if(CollectionUtils.isNotEmpty(newRoleCodeList)){
            List<AclAuthority> saveList = newRoleCodeList.stream().map(code -> {
                AclAuthority aclAuthority = new AclAuthority();
                aclAuthority.setUsername(username);
                aclAuthority.setAuthority(code);
                aclAuthority.setCreateTime(now);
                aclAuthority.setUpdateTime(now);
                return aclAuthority;
            }).toList();
            aclAuthorityRepository.saveAll(saveList);
        }
    }

    /**
     * 获取登录用户的session集合
     * @param username
     * @return
     */
    public Collection<? extends Session> getSessions(String username) {
        return findByIndexNameSessionRepository.findByPrincipalName(username).values();
    }

    /**
     * 剔除拥有特定权限的所有登录用户的所有Session
     * @param authorityItemCodes
     */
    public void removeSession(List<String> authorityItemCodes){
        if(CollectionUtils.isEmpty(authorityItemCodes)){
            return;
        }

        String lastUsername = StringUtils.EMPTY;
        Pageable pageable = PageRequest.of(0, 1000, Sort.by("username"));
        Page<String> page = aclAuthorityRepository.scanDistinctUsernameByAuthorityIn(lastUsername, authorityItemCodes, pageable);
        while(page.hasContent()){
            for (String username : page.getContent()) {
                this.removeSession(username);
            }
            lastUsername = page.getContent().getLast();
            page = aclAuthorityRepository.scanDistinctUsernameByAuthorityIn(lastUsername, authorityItemCodes, pageable);
        }
    }

    /**
     * 剔除登录用户的所有Session
     * @param username
     */
    public void removeSession(String username){
        Set<String> usersSessionIds = findByIndexNameSessionRepository.findByPrincipalName(username).keySet();
        if(CollectionUtils.isNotEmpty(usersSessionIds)){
            this.removeSession(username, usersSessionIds);
        }
    }

    /**
     * 剔除登录用户的指定Session
     * @param username
     * @param sessionIdsToDelete
     */
    public void removeSession(String username, Collection<String> sessionIdsToDelete) {
        Assert.notEmpty(sessionIdsToDelete, "sessionIdsToDelete cannot be empty");

        Set<String> usersSessionIds = findByIndexNameSessionRepository.findByPrincipalName(username).keySet();
        for (String sessionIdToDelete : sessionIdsToDelete) {
            if (usersSessionIds.contains(sessionIdToDelete)) {
                findByIndexNameSessionRepository.deleteById(sessionIdToDelete);
            }
        }
    }

    private void deleteUserAuthorities(String username) {
        aclAuthorityRepository.deleteByUsername(username);
    }
}
