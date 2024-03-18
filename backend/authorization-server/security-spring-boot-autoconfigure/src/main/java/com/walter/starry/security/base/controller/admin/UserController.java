package com.walter.starry.security.base.controller.admin;

import com.walter.starry.security.base.bo.AclUserBo;
import com.walter.starry.security.base.common.enums.SystemRoleEnum;
import com.walter.starry.security.base.component.security.JpaUserDetailsService;
import com.walter.starry.security.base.controller.AbstractBaseController;
import com.walter.starry.security.base.entity.AclAuthority;
import com.walter.starry.security.base.entity.AclUser;
import com.walter.starry.security.base.repository.AclAuthorityRepository;
import com.walter.starry.security.base.repository.AclUserRepository;
import com.walter.starry.security.base.service.AuthorityItemService;
import com.walter.starry.security.base.util.IdUtil;
import com.walter.starry.security.base.vo.request.user.*;
import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.vo.response.user.UserAvailableAuthorityResponse;
import com.walter.starry.security.base.vo.response.user.UserResponse;
import com.walter.starry.security.base.vo.response.user.UserSessionResponse;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理
 * @author: walter.tan
 * @datetime: 2023/9/27 16:41
 */
@RestController
@RequestMapping("/admin/user")
public class UserController extends AbstractBaseController {
    @Autowired
    private AclUserRepository aclUserRepository;
    @Autowired
    private AclAuthorityRepository aclAuthorityRepository;
    @Autowired
    private JpaUserDetailsService jpaUserDetailsService;
    @Autowired
    private AuthorityItemService authorityItemService;

    /**
     * 用户列表
     * @param req
     * @return
     */
    @PostMapping("/list")
    public ApiResponse<Page<UserResponse>> list(@RequestBody ListUserRequest req){
        Specification<AclUser> spec = (root, query, builder) -> {
            List<Predicate> andPredicates = new ArrayList<>();
            if(StringUtils.isNotBlank(req.getUsername())){
                andPredicates.add(builder.equal(root.get("username"), req.getUsername()));
            }
            if(StringUtils.isNotBlank(req.getNickname())){
                andPredicates.add(builder.like(root.get("nickname"), "%" + req.getNickname() + "%"));
            }
            if(Objects.nonNull(req.getEnabled())){
                andPredicates.add(builder.equal(root.get("enabled"), req.getEnabled()));
            }
            if(Objects.nonNull(req.getCreateTimeBegin())){
                andPredicates.add(builder.greaterThanOrEqualTo(root.get("createTime"), req.getCreateTimeBegin()));
            }
            if(Objects.nonNull(req.getCreateTimeEnd())){
                andPredicates.add(builder.lessThanOrEqualTo(root.get("createTime"), req.getCreateTimeEnd()));
            }
            return builder.and(andPredicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(req.getPageNumber(), req.getPageSize(), Sort.by("createTime").descending().and(Sort.by("username")));
        Page<AclUser> page = aclUserRepository.findAll(spec, pageable);

        List<UserResponse> list = page.getContent().stream().map(u -> {
            UserResponse userResponse = new UserResponse();
            BeanUtils.copyProperties(u, userResponse);
            userResponse.setExpiredSessionsCleanTime(u.getExpiredSessionsCleanTime().getTime());
            userResponse.setCreateTime(u.getCreateTime().getTime());
            userResponse.setUpdateTime(u.getUpdateTime().getTime());
            userResponse.setPassword(null);
            return userResponse;
        }).toList();

        return ApiResponse.success(new PageImpl<>(list, pageable, page.getTotalElements()));
    }

    /**
     * 保存用户
     * @param req
     * @param bindingResult
     * @return
     */
    @PostMapping("/save")
    public ApiResponse<Void> save(@Validated @RequestBody SaveUserRequest req, BindingResult bindingResult){
        return super.apiCall("save", bindingResult, () -> {
            if(StringUtils.isBlank(req.getUsername())){
                // 新增
                String username = UUID.randomUUID().toString().replaceAll("-", "");
                AclUserBo aclUserBo = new AclUserBo(IdUtil.genNextGlobalId(), username, req.getNickname(), null, null, null,
                        true, true, true, req.getEnabled(), new HashSet<>());
                jpaUserDetailsService.createUser(aclUserBo);
            }else{
                // 修改
                AclUser example = new AclUser();
                example.setUsername(req.getUsername());
                Optional<AclUser> userOptional = aclUserRepository.findOne(Example.of(example));
                userOptional.ifPresent(user -> {
                    user.setNickname(req.getNickname());
                    user.setEnabled(req.getEnabled());
                    aclUserRepository.save(user);
                });
            }

            return null;
        });
    }

    /**
     * 删除用户
     * @param req
     * @return
     */
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Validated @RequestBody DeleteUserRequest req, BindingResult bindingResult){
        return super.apiCall("delete", bindingResult, () -> {
            for (String username : req.getUsernameList()) {
                jpaUserDetailsService.deleteUser(username);
            }
            return null;
        });
    }

    /**
     * 获取用户session集合
     * @param username
     * @return
     */
    @GetMapping("/getSessions")
    public ApiResponse<List<UserSessionResponse>> getSessions(String username){
        return super.apiCall("getSessions", null,
            () -> jpaUserDetailsService.getSessions(username).stream().map(session -> {
                UserSessionResponse res = new UserSessionResponse();
                res.setSessionId(session.getId());
                res.setCreateTime(session.getCreationTime().toEpochMilli());
                res.setLastAccessedTime(session.getLastAccessedTime().toEpochMilli());
                res.setMaxInactiveInterval(session.getMaxInactiveInterval().toMillis());
                res.setExpired(session.isExpired());
                return res;
            }).toList());
    }

    /**
     * 剔除登录用户的指定Session
     * @param req
     * @param bindingResult
     * @return
     */
    @PostMapping("/removeSession")
    public ApiResponse<Void> removeSession(@Validated @RequestBody DeleteSessionRequest req, BindingResult bindingResult){
        return super.apiCall("removeSession", bindingResult, () -> {
            jpaUserDetailsService.removeSession(req.getUsername(), req.getSessionIds());
            return null;
        });
    }

    /**
     * 获取当前用户对目标用户的可选择的权限树列表
     * @param username 目标用户的账号
     * @param context 当前用户的上下文
     * @return
     */
    @GetMapping("/authority/listAvailableTree")
    public ApiResponse<List<UserAvailableAuthorityResponse>> listAvailableAuthorityTree(@RequestParam("username") String username, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("listAvailableAuthorityTree", null, () -> {
            // 获取当前用户的权限
            final Set<String> userAuthorityCodes = context.getAuthentication().getAuthorities()
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            // 当前登录用户可对目标用户授权的根权限集
            final Set<String> userAuthorityCodeChoices;
            if(userAuthorityCodes.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                userAuthorityCodeChoices = userAuthorityCodes;
            }else{
                // 非系统管理员，不允许给用户授权系统角色
                userAuthorityCodeChoices = CollectionUtils.subtract(userAuthorityCodes, SystemRoleEnum.getSystemRoleCodes()).stream().collect(Collectors.toUnmodifiableSet());
            }

            if(CollectionUtils.isEmpty(userAuthorityCodeChoices)){
                return new ArrayList<>();
            }

            // 查找目标用户已拥有的权限
            AclAuthority aclAuthorityExample = new AclAuthority();
            aclAuthorityExample.setUsername(username);
            final Set<String> targetUserExistAuthoritySet = aclAuthorityRepository.findAll(Example.of(aclAuthorityExample))
                    .stream().map(AclAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            try {
                List<UserAvailableAuthorityResponse> resultList = new ArrayList<>(userAuthorityCodeChoices.size());

                // 获取全部权限树及全部权限集
                Map<String, UserAvailableAuthorityResponse> allAuthorityItemMap = new HashMap<>();
                List<UserAvailableAuthorityResponse> rootAuthorityList = authorityItemService.getAllAuthorityTrees(UserAvailableAuthorityResponse.class, allAuthorityItemMap::putAll);

                // 遍历权限森林
                Queue<UserAvailableAuthorityResponse> travelingQueue = new LinkedList<>();
                rootAuthorityList.forEach(travelingQueue::offer);
                while(!travelingQueue.isEmpty()){
                    UserAvailableAuthorityResponse head = travelingQueue.poll();
                    if(userAuthorityCodeChoices.contains(head.getCode())){
                        // 按原顺序把当前操作用户可选择的权限项，添加到结果列表中
                        resultList.add(head);
                    }

                    if(targetUserExistAuthoritySet.contains(head.getCode())){
                        // 目标用户已拥有该权限
                        head.setGranted(true);
                    }

                    head.getChildren().forEach(child -> travelingQueue.offer((UserAvailableAuthorityResponse) child));
                }

                // 在结果列表中剔除存在包含关系的子权限树
                for (int i = 0; i < resultList.size(); i++) {
                    UserAvailableAuthorityResponse availableAuthority = resultList.get(i);
                    availableAuthority.setParentNameList(new LinkedList<>());

                    // 回溯迭代
                    UserAvailableAuthorityResponse curr = availableAuthority;
                    while (Objects.nonNull(curr.getParentCode()) && !curr.getParentCode().equals(curr.getCode())){
                        UserAvailableAuthorityResponse parent = allAuthorityItemMap.get(curr.getParentCode());
                        availableAuthority.getParentNameList().add(0, parent.getName());

                        if(userAuthorityCodeChoices.contains(parent.getCode())){
                            // 发现包含关系，剔除
                            resultList.set(i, null);
                            break;
                        }else{
                            curr = parent;
                        }
                    }
                }

                return resultList.stream().filter(Objects::nonNull).toList();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 用户授权保存
     * @param req
     * @param bindingResult
     * @return
     */
    @PostMapping("/authority/grant")
    public ApiResponse<Void> grantAuthority(@Validated @RequestBody GrantAuthorityRequest req, BindingResult bindingResult){
        return super.apiCall("grantAuthority", bindingResult, () -> {
            // 保存权限
            jpaUserDetailsService.grantAuthority(req.getUsername(), req.getNewRoleCodeList(), req.getRemoveRoleCodeList());

            // 踢出目标用户的会话
            jpaUserDetailsService.removeSession(req.getUsername());
            return null;
        });
    }
}
