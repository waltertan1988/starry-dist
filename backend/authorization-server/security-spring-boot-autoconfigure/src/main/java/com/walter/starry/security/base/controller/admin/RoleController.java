package com.walter.starry.security.base.controller.admin;

import com.walter.starry.security.base.controller.AbstractBaseController;
import com.walter.starry.security.base.vo.request.role.ListTreeRequest;
import com.walter.starry.security.base.vo.request.role.MoveRoleRequest;
import com.walter.starry.security.base.vo.request.role.SaveRoleRequest;
import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.common.enums.SystemRoleEnum;
import com.walter.starry.security.base.common.exception.BizException;
import com.walter.starry.security.base.component.security.OpenPolicyAgentAuthorizationManager;
import com.walter.starry.security.base.entity.AclAuthorityItem;
import com.walter.starry.security.base.repository.AclAuthorityItemRepository;
import com.walter.starry.security.base.service.AuthorityItemService;
import com.walter.starry.security.base.vo.response.role.AuthorityItemResponse;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理
 * @Author: walter.tan
 * @DateTime: 2023-10-11 09:19:56
 */
@RestController
@RequestMapping("/admin/role")
public class RoleController extends AbstractBaseController {
    public static final String ROLE_PREFIX = "ROLE_";

    @Autowired
    private OpenPolicyAgentAuthorizationManager openPolicyAgentAuthorizationManager;
    @Autowired
    private AclAuthorityItemRepository aclAuthorityItemRepository;
    @Autowired
    private AuthorityItemService authorityItemService;

    /**
     * 查询角色树树根（即角色森林）列表
     * @return
     */
    @PostMapping("/listTree")
    public ApiResponse<List<AuthorityItemResponse>> listTree(@RequestBody ListTreeRequest req, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("listTree", null, () -> {
            // 获取当前用户可达的权限集合
            final Set<String> userReachableAuthorityCodes = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            Specification<AclAuthorityItem> spec = (root, query, builder) -> {
                List<Predicate> andPredicates = new ArrayList<>();
                if(StringUtils.isNotBlank(req.getCode())){
                    andPredicates.add(builder.like(root.get("code").as(String.class), "%" + req.getCode() + "%"));
                }
                if(StringUtils.isNotBlank(req.getName())){
                    andPredicates.add(builder.like(root.get("name").as(String.class), "%" + req.getName() + "%"));
                }
                if(Objects.nonNull(req.getSystemAuthority())){
                    andPredicates.add(builder.equal(root.get("systemAuthority").as(Boolean.class), req.getSystemAuthority()));
                }
                if(!userReachableAuthorityCodes.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                    // 非系统管理员，不允许看到系统角色
                    andPredicates.add(builder.equal(root.get("systemAuthority").as(Boolean.class), false));
                }

                return builder.and(andPredicates.toArray(new Predicate[0]));
            };
            Sort sort = Sort.by("systemAuthority").descending()
                    .and(Sort.by("parentCode"))
                    .and(Sort.by("priority"))
                    .and(Sort.by("id"));

            // 用户只能查看自身权限内的角色
            List<AclAuthorityItem> authorityItemList = aclAuthorityItemRepository.findAll(spec, sort)
                    .stream().filter(item -> userReachableAuthorityCodes.contains(item.getCode())).toList();

            try {
                return authorityItemService.getAuthorityTrees(AuthorityItemResponse.class, authorityItemList);
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * 保存角色
     * @param req
     * @param bindingResult
     * @return
     */
    @PostMapping("/save")
    public ApiResponse<Void> save(@Validated @RequestBody SaveRoleRequest req, BindingResult bindingResult){
        return super.apiCall("save", bindingResult, () -> {
            if(!req.getCode().startsWith(ROLE_PREFIX)){
                throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, String.format("角色编码必须以%s开头", ROLE_PREFIX));
            }

            if(Objects.isNull(req.getId())){
                // 新增
                authorityItemService.create(req);
            }else{
                // 修改
                authorityItemService.update(req);
            }
            return null;
        });
    }

    /**
     * 删除角色（包括所有下级角色）
     * @param code
     * @return
     */
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam("code") String code){
        return super.apiCall("delete", null, () -> {
            // 获取待删除角色的所有可达角色的编码集合
            List<String> reachableCodes = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(Collections.singleton(new SimpleGrantedAuthority(code)))
                    .stream().map(GrantedAuthority::getAuthority).toList();

            authorityItemService.delete(reachableCodes);

            return null;
        });
    }

    /**
     * 移动角色
     * @param req
     * @param bindingResult
     * @return
     */
    @PostMapping("/move")
    public ApiResponse<Void> move(@Validated @RequestBody MoveRoleRequest req, BindingResult bindingResult){
        return super.apiCall("move", bindingResult, () -> {
            // 获取待移动角色的所有可达角色的编码集合
            Collection<String> reachableCodes = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(Collections.singleton(new SimpleGrantedAuthority(req.getCode())))
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

            if(reachableCodes.contains(req.getMoveToCode())){
                throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "不允许移动到自己或自己的下级角色下面");
            }

            authorityItemService.move(req);

            return null;
        });
    }
}
