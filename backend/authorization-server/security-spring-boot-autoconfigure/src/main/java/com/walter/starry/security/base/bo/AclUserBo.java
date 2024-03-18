package com.walter.starry.security.base.bo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;

/**
 * @author: walter.tan
 * @datetime: 2023/9/14 17:54
 * @description: 用@JsonTypeInfo注解，解决使用Jackson反序列化时的报错，解决方法参见：
 *  https://blog.csdn.net/m13012606980/article/details/125291005
 *  https://segmentfault.com/a/1190000040943737?sort=votes
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class AclUserBo implements UserDetails, CredentialsContainer {

    private Long id;

    private String username;

    private String nickname;

    private String password;

    private String oauth2RegistrationId;

    private String openId;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private boolean credentialsNonExpired;

    private boolean enabled;

    private Date expiredSessionsCleanTime;

    private Date createTime;

    private Date updateTime;

    private Collection<GrantedAuthority> authorities;

    public AclUserBo(){
    }

    public AclUserBo(Long id, String username, String nickname, String password, String oauth2RegistrationId, String openId,
                     boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled,
                     Collection<GrantedAuthority> authorities) {
        this(id, username, nickname, password, oauth2RegistrationId, openId,
                accountNonExpired, accountNonLocked, credentialsNonExpired, enabled,
                null, null, null, authorities);
    }

    public AclUserBo(Long id, String username, String nickname, String password, String oauth2RegistrationId, String openId,
                     boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired, boolean enabled,
                     Date expiredSessionsCleanTime, Date createTime, Date updateTime,
                     Collection<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.oauth2RegistrationId = oauth2RegistrationId;
        this.openId = openId;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.expiredSessionsCleanTime = expiredSessionsCleanTime;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getOauth2RegistrationId() {
        return oauth2RegistrationId;
    }

    public String getOpenId() {
        return openId;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    public Date getExpiredSessionsCleanTime() {
        return expiredSessionsCleanTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAuthorities(Set<GrantedAuthority> authorities) {
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setExpiredSessionsCleanTime(Date expiredSessionsCleanTime) {
        this.expiredSessionsCleanTime = expiredSessionsCleanTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    private static SortedSet<GrantedAuthority> sortAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "Cannot pass a null GrantedAuthority collection");
        // Ensure array iteration order is predictable (as per
        // UserDetails.getAuthorities() contract and SEC-717)
        SortedSet<GrantedAuthority> sortedAuthorities = new TreeSet<>(new AuthorityComparator());
        for (GrantedAuthority grantedAuthority : authorities) {
            Assert.notNull(grantedAuthority, "GrantedAuthority list cannot contain any null elements");
            sortedAuthorities.add(grantedAuthority);
        }
        return sortedAuthorities;
    }

    /**
     * Returns {@code true} if the supplied object is a {@code User} instance with the
     * same {@code username} value.
     * <p>
     * In other words, the objects are equal if they have the same username, representing
     * the same principal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AclUserBo aclUserBo = (AclUserBo) o;
        return Objects.equals(username, aclUserBo.username);
    }

    /**
     * Returns the hashcode of the {@code username}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(" [");
        sb.append("Username=").append(this.username).append(", ");
        sb.append("Nickname=").append(this.nickname).append(", ");
        sb.append("Password=[PROTECTED], ");
        sb.append("Enabled=").append(this.enabled).append(", ");
        sb.append("AccountNonExpired=").append(this.accountNonExpired).append(", ");
        sb.append("credentialsNonExpired=").append(this.credentialsNonExpired).append(", ");
        sb.append("AccountNonLocked=").append(this.accountNonLocked).append(", ");
        sb.append("Granted Authorities=").append(this.authorities).append("]");
        return sb.toString();
    }

    private static class AuthorityComparator implements Comparator<GrantedAuthority>, Serializable {

        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

        @Override
        public int compare(GrantedAuthority g1, GrantedAuthority g2) {
            // Neither should ever be null as each entry is checked before adding it to
            // the set. If the authority is null, it is a custom authority and should
            // precede others.
            if (g2.getAuthority() == null) {
                return -1;
            }
            if (g1.getAuthority() == null) {
                return 1;
            }
            return g1.getAuthority().compareTo(g2.getAuthority());
        }
    }
}
