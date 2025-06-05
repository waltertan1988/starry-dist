package com.walter.starry.security.base.controller.admin;

import com.google.common.collect.Lists;
import com.walter.starry.security.base.common.enums.SystemRoleEnum;
import com.walter.starry.security.base.common.exception.BizException;
import com.walter.starry.security.base.component.security.OpenPolicyAgentAuthorizationManager;
import com.walter.starry.security.base.controller.AbstractBaseController;
import com.walter.starry.security.base.entity.AclAuthorityResource;
import com.walter.starry.security.base.entity.AclResourceGroup;
import com.walter.starry.security.base.entity.AclResourceItem;
import com.walter.starry.security.base.repository.AclAuthorityResourceRepository;
import com.walter.starry.security.base.repository.AclResourceGroupRepository;
import com.walter.starry.security.base.repository.AclResourceItemRepository;
import com.walter.starry.security.base.service.AuthorityItemService;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.vo.request.menu.*;
import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.vo.response.resource.ResourceAuthorityResponse;
import com.walter.starry.security.base.vo.response.resource.ResourceGroupVo;
import com.walter.starry.security.base.vo.response.resource.ResourceItemVo;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单管理
 * @author: walter.tan
 * @datetime: 2023/9/14 20:46
 */
@RestController
@RequestMapping("/admin/menu")
public class MenuController extends AbstractBaseController {
    @Autowired
    private AclResourceGroupRepository aclResourceGroupRepository;
    @Autowired
    private AclResourceItemRepository aclResourceItemRepository;
    @Autowired
    private AclAuthorityResourceRepository aclAuthorityResourceRepository;
    @Autowired
    private OpenPolicyAgentAuthorizationManager openPolicyAgentAuthorizationManager;
    @Autowired
    private ResourceGroupService resourceGroupService;
    @Autowired
    private AuthorityItemService authorityItemService;

    /**
     * 获取登录用户指定菜单分组下的有权限访问的完整菜单树结构
     * (获取Anonymous Authentication的方式参考：https://docs.spring.io/spring-security/reference/servlet/authentication/anonymous.html#anonymous-auth-mvc-controller)
     * @param rootResourceGroupCode 资源树所在的根分组
     * @param context 用户认证上下文
     * @return
     */
    @GetMapping("/loadForAllUser")
    public ApiResponse<ResourceGroupVo> loadForAllUser(String rootResourceGroupCode, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("loadForAllUser", null, () -> {
            // 获取Authentication（要获取匿名认证，必须使用方法参数：@CurrentSecurityContext SecurityContext context）
            Authentication authentication = context.getAuthentication();

            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(authentication.getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            // 生成并获取指定类型的资源树
            return resourceGroupService.generateResourceGroupTree(AclResourceGroup.TypeEnum.MENU, rootResourceGroupCode, grantedAuthorities);
        });
    }

    /**
     * 管理员查看所有菜单列表
     * @param req 查询参数
     * @param context 用户认证上下文
     * @return
     */
    @PostMapping("/list")
    public ApiResponse<List<ResourceGroupVo>> list(@Validated @RequestBody ListMenuRequest req, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("list", null, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限查看菜单列表");
            }

            final List<ResourceGroupVo> menuRowList = new ArrayList<>();

            // 需要从菜单组查询
            if(Objects.isNull(req.getRowType()) || ResourceGroupVo.RowTypeEnum.GROUP.getCode().equals(req.getRowType())){
                // 筛选菜单组
                Specification<AclResourceGroup> spec = (root, query, builder) -> {
                    List<Predicate> andPredicates = new ArrayList<>();
                    if(StringUtils.isNotBlank(req.getCode())){
                        andPredicates.add(builder.like(root.get("code"), "%" + req.getCode() + "%"));
                    }
                    if(StringUtils.isNotBlank(req.getName())){
                        andPredicates.add(builder.like(root.get("name"), "%" + req.getName() + "%"));
                    }
                    andPredicates.add(builder.equal(root.get("type"), AclResourceGroup.TypeEnum.MENU.getCode()));

                    return builder.and(andPredicates.toArray(new Predicate[0]));
                };
                List<AclResourceGroup> aclResourceGroupList = aclResourceGroupRepository.findAll(spec);
                for (AclResourceGroup aclGroup : aclResourceGroupList) {
                    ResourceGroupVo vo = new ResourceGroupVo();
                    vo.setRowType(ResourceGroupVo.RowTypeEnum.GROUP.getCode());
                    vo.setId(aclGroup.getId());
                    vo.setCode(aclGroup.getCode());
                    vo.setName(aclGroup.getName());
                    vo.setSeq(aclGroup.getSeq());
                    vo.setParentGroupCode(aclGroup.getParentGroupCode());
                    vo.setConfig(JsonUtil.toBean(aclGroup.getConfig(), AclResourceGroup.MenuConfig.class));
                    vo.setCreateTime(aclGroup.getCreateTime());
                    vo.setUpdateTime(aclGroup.getUpdateTime());
                    vo.setResourceGroupVoList(new ArrayList<>());

                    menuRowList.add(vo);
                }
            }

            // 需要从菜单项查询
            if(Objects.isNull(req.getRowType()) || ResourceGroupVo.RowTypeEnum.ITEM.getCode().equals(req.getRowType())){
                // 先找出资源组类型为“菜单”的分组编码
                AclResourceGroup groupExample = new AclResourceGroup();
                groupExample.setType(AclResourceGroup.TypeEnum.MENU.getCode());
                List<String> allMenuGroupCodes = aclResourceGroupRepository.findAll(Example.of(groupExample)).stream().map(AclResourceGroup::getCode).toList();
                Assert.notEmpty(allMenuGroupCodes, "cannot find any menu groups");

                // 筛选菜单项
                for (List<String> menuGroupCodes : Lists.partition(allMenuGroupCodes, 10)) {
                    Specification<AclResourceItem> spec = (root, query, builder) -> {
                        List<Predicate> andPredicates = new ArrayList<>();
                        if(StringUtils.isNotBlank(req.getCode())){
                            andPredicates.add(builder.like(root.get("code"), "%" + req.getCode() + "%"));
                        }
                        if(StringUtils.isNotBlank(req.getName())){
                            andPredicates.add(builder.like(root.get("name"), "%" + req.getName() + "%"));
                        }
                        andPredicates.add(builder.in(root.get("parentGroupCode")).value(menuGroupCodes));

                        return builder.and(andPredicates.toArray(new Predicate[0]));
                    };
                    List<AclResourceItem> aclResourceItemList = aclResourceItemRepository.findAll(spec);
                    for (AclResourceItem aclItem : aclResourceItemList) {
                        ResourceItemVo vo = new ResourceItemVo();
                        vo.setRowType(ResourceGroupVo.RowTypeEnum.ITEM.getCode());
                        vo.setId(aclItem.getId());
                        vo.setCode(aclItem.getCode());
                        vo.setHttpMethodList(Lists.newArrayList(aclItem.getHttpMethodList().split(OpenPolicyAgentAuthorizationManager.HTTP_METHOD_LIST_DELIMITER)));
                        vo.setPattern(aclItem.getPattern());
                        vo.setName(aclItem.getName());
                        vo.setSeq(aclItem.getSeq());
                        vo.setParentGroupCode(aclItem.getParentGroupCode());
                        vo.setConfig(JsonUtil.toBean(aclItem.getConfig(), AclResourceItem.Config.class));
                        vo.setCreateTime(aclItem.getCreateTime());
                        vo.setUpdateTime(aclItem.getUpdateTime());

                        menuRowList.add(vo);
                    }
                }
            }

            // 生成资源子树树根集合
            return resourceGroupService.getResourceTrees(new UnmodifiableList<>(menuRowList));
        });
    }

    /**
     * 保存菜单分组
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/saveGroup")
    public ApiResponse<Void> saveGroup(@Validated @RequestBody SaveMenuGroupRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("saveGroup", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            AclResourceGroup.MenuConfig config = new AclResourceGroup.MenuConfig();
            BeanUtils.copyProperties(req.getConfig(), config);

            AclResourceGroup groupRequest = new AclResourceGroup();
            BeanUtils.copyProperties(req, groupRequest);
            groupRequest.setType(AclResourceGroup.TypeEnum.MENU.getCode());
            groupRequest.setConfig(JsonUtil.toJson(config));

            if(Objects.isNull(groupRequest.getId())){
                // 新增
                resourceGroupService.createGroup(groupRequest);
            }else{
                // 修改
                resourceGroupService.updateGroup(groupRequest);
            }

            return null;
        });
    }

    /**
     * 保存菜单项
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/saveItem")
    public ApiResponse<Void> saveItem(@Validated @RequestBody SaveMenuItemRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("saveItem", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            AclResourceItem.Config config = new AclResourceItem.Config();
            BeanUtils.copyProperties(req.getConfig(), config);

            AclResourceItem itemRequest = new AclResourceItem();
            BeanUtils.copyProperties(req, itemRequest);
            itemRequest.setHttpMethodList(HttpMethod.GET.name());
            itemRequest.setConfig(JsonUtil.toJson(config));

            if(Objects.isNull(itemRequest.getId())){
                // 新增
                resourceGroupService.createItem(itemRequest);
            }else{
                // 修改
                resourceGroupService.updateItem(itemRequest);
            }

            return null;
        });
    }

    /**
     * 移动菜单行子树
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/move")
    public ApiResponse<Void> move(@Validated @RequestBody MoveMenuRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("move", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            if(Objects.equals(ResourceGroupVo.RowTypeEnum.GROUP.getCode(), req.getRowType())){
                resourceGroupService.moveGroup(AclResourceGroup.TypeEnum.MENU, req.getCode(), req.getMoveToGroupCode());
            }else{
                resourceGroupService.moveItem(req.getCode(), req.getMoveToGroupCode());
            }
            return null;
        });
    }

    /**
     * 删除菜单行子树
     * @param rowType
     * @param code
     * @param context
     * @return
     */
    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestParam("rowType") Integer rowType, @RequestParam("code") String code, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("delete", null, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            if(Objects.equals(ResourceGroupVo.RowTypeEnum.GROUP.getCode(), rowType)){
                // 删除菜单分组

                // 找出全部菜单分组
                AclResourceGroup example = new AclResourceGroup();
                example.setType(AclResourceGroup.TypeEnum.MENU.getCode());
                Map<String, ResourceGroupVo> groupVoMap = aclResourceGroupRepository.findAll(Example.of(example))
                        .stream().map(aclGroup -> {
                            ResourceGroupVo groupVo = new ResourceGroupVo();
                            groupVo.setCode(aclGroup.getCode());
                            groupVo.setParentGroupCode(aclGroup.getParentGroupCode());
                            groupVo.setResourceGroupVoList(new ArrayList<>());
                            return groupVo;
                        })
                        .collect(Collectors.toMap(ResourceGroupVo::getCode, Function.identity()));

                if(!groupVoMap.containsKey(code)){
                    throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "菜单分组不存在：" + code);
                }

                // 关联各个菜单分组的子分组
                for (ResourceGroupVo groupVo : groupVoMap.values()) {
                    if(StringUtils.isNotBlank(groupVo.getParentGroupCode()) && !groupVo.getParentGroupCode().equals(groupVo.getCode())
                            && groupVoMap.containsKey(groupVo.getParentGroupCode())){
                        groupVoMap.get(groupVo.getParentGroupCode()).getResourceGroupVoList().add(groupVo);
                    }
                }

                // 查找待删除的菜单分组树
                final List<String> deleteGroupCodes = Lists.newArrayList();
                final Queue<ResourceGroupVo> deleteGroupQueue = Lists.newLinkedList();
                deleteGroupQueue.add(groupVoMap.get(code));
                while (!deleteGroupQueue.isEmpty()){
                    ResourceGroupVo head = deleteGroupQueue.poll();
                    deleteGroupCodes.add(head.getCode());

                    if(CollectionUtils.isNotEmpty(head.getResourceGroupVoList())){
                        deleteGroupQueue.addAll(head.getResourceGroupVoList());
                    }
                }

                // 找出待删分组对应的菜单项
                Specification<AclResourceItem> spec = (root, query, builder) -> {
                    List<Predicate> andPredicates = new ArrayList<>();
                    andPredicates.add(builder.in(root.get("parentGroupCode")).value(deleteGroupCodes));
                    return builder.and(andPredicates.toArray(new Predicate[0]));
                };
                List<String> deleteItemCodes = aclResourceItemRepository.findAll(spec).stream().map(AclResourceItem::getCode).toList();

                // 删除菜单分组及对应的菜单项
                resourceGroupService.deleteGroupsAndItems(AclResourceGroup.TypeEnum.MENU, deleteGroupCodes, deleteItemCodes);
            }else{
                // 删除菜单项
                resourceGroupService.deleteGroupsAndItems(null, null, Lists.newArrayList(code));
            }
            return null;
        });
    }

    /**
     * 获取当前菜单项的可选权限树列表
     * @param menuItemCode
     * @param context
     * @return
     */
    @PostMapping("/authority/listTree")
    public ApiResponse<List<ResourceAuthorityResponse>> listAuthorityTree(@RequestParam("menuItemCode") String menuItemCode, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("listAuthorityTree", null, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            try {
                // 获取全部权限树及全部权限集
                Map<String, ResourceAuthorityResponse> allAuthorityItemMap = new HashMap<>();
                List<ResourceAuthorityResponse> rootAuthorityList = authorityItemService.getAllAuthorityTrees(ResourceAuthorityResponse.class, allAuthorityItemMap::putAll);

                // 获取菜单项已关联的权限集合
                Set<String> grantedAuthorityCodes = aclAuthorityResourceRepository.findByResourceItemCodeIn(Lists.newArrayList(menuItemCode))
                        .stream().map(AclAuthorityResource::getAuthorityItemCode).collect(Collectors.toSet());

                // 设置已关联的权限
                for (String grantedAuthorityCode : grantedAuthorityCodes) {
                    ResourceAuthorityResponse resourceAuthorityResponse = allAuthorityItemMap.get(grantedAuthorityCode);
                    if(Objects.nonNull(resourceAuthorityResponse)){
                        resourceAuthorityResponse.setGranted(true);
                    }
                }

                return rootAuthorityList;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * 菜单项授权
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/authority/grant")
    public ApiResponse<Void> grantAuthority(@Validated @RequestBody MenuGrantAuthorityRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("grantAuthority", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            resourceGroupService.grantResourceAuthorities(req.getMenuItemCode(), req.getNewRoleCodeList(), req.getRemoveRoleCodeList());

            return null;
        });
    }
}