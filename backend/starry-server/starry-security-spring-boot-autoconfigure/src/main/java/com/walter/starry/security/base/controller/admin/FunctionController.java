package com.walter.starry.security.base.controller.admin;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.walter.starry.common.exception.BizException;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.vo.ApiResponse;
import com.walter.starry.security.base.common.enums.SystemRoleEnum;
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
import com.walter.starry.security.base.vo.request.function.*;
import com.walter.starry.security.base.vo.response.resource.ResourceAuthorityResponse;
import com.walter.starry.security.base.vo.response.resource.ResourceGroupVo;
import com.walter.starry.security.base.vo.response.resource.ResourceItemVo;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpMethod;
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
 * 功能管理
 * @Author: walter.tan
 * @DateTime: 2023-11-05 21:35:13
 */
@RestController
@RequestMapping("/admin/function")
public class FunctionController extends AbstractBaseController {
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
     * 管理员查看所有功能列表
     * @param req 查询参数
     * @param context 用户认证上下文
     * @return
     */
    @PostMapping("/list")
    public ApiResponse<List<ResourceGroupVo>> list(@Validated @RequestBody ListFunctionRequest req, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("list", null, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限查看功能列表");
            }

            final List<ResourceGroupVo> functionRowList = new ArrayList<>();

            // 需要从功能组查询
            if(Objects.isNull(req.getRowType()) || ResourceGroupVo.RowTypeEnum.GROUP.getCode().equals(req.getRowType())){
                // 筛选功能组
                Specification<AclResourceGroup> spec = (root, query, builder) -> {
                    List<Predicate> andPredicates = new ArrayList<>();
                    if(StringUtils.isNotBlank(req.getCode())){
                        andPredicates.add(builder.like(root.get("code"), "%" + req.getCode() + "%"));
                    }
                    if(StringUtils.isNotBlank(req.getName())){
                        andPredicates.add(builder.like(root.get("name"), "%" + req.getName() + "%"));
                    }
                    andPredicates.add(builder.equal(root.get("type"), AclResourceGroup.TypeEnum.FUNCTION.getCode()));

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
                    vo.setConfig(JsonUtil.toBean(aclGroup.getConfig(), AclResourceGroup.FunctionConfig.class));
                    vo.setCreateTime(aclGroup.getCreateTime());
                    vo.setUpdateTime(aclGroup.getUpdateTime());
                    vo.setResourceGroupVoList(new ArrayList<>());

                    functionRowList.add(vo);
                }
            }

            // 需要从功能项查询
            if(Objects.isNull(req.getRowType()) || ResourceGroupVo.RowTypeEnum.ITEM.getCode().equals(req.getRowType())){
                // 先找出资源组类型为“功能”的分组编码
                AclResourceGroup groupExample = new AclResourceGroup();
                groupExample.setType(AclResourceGroup.TypeEnum.FUNCTION.getCode());
                List<String> allFunctionGroupCodes = aclResourceGroupRepository.findAll(Example.of(groupExample)).stream().map(AclResourceGroup::getCode).toList();
                Assert.notEmpty(allFunctionGroupCodes, "cannot find any function groups");

                // 筛选功能项
                for (List<String> functionGroupCodes : Lists.partition(allFunctionGroupCodes, 10)) {
                    Specification<AclResourceItem> spec = (root, query, builder) -> {
                        List<Predicate> andPredicates = new ArrayList<>();
                        if(StringUtils.isNotBlank(req.getCode())){
                            andPredicates.add(builder.like(root.get("code"), "%" + req.getCode() + "%"));
                        }
                        if(StringUtils.isNotBlank(req.getName())){
                            andPredicates.add(builder.like(root.get("name"), "%" + req.getName() + "%"));
                        }
                        andPredicates.add(builder.in(root.get("parentGroupCode")).value(functionGroupCodes));

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

                        functionRowList.add(vo);
                    }
                }
            }

            // 生成资源子树树根集合
            return resourceGroupService.getResourceTrees(new UnmodifiableList<>(functionRowList));
        });
    }

    /**
     * 保存功能分组
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/saveGroup")
    public ApiResponse<Void> saveGroup(@Validated @RequestBody SaveFunctionGroupRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("saveGroup", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            AclResourceGroup.FunctionConfig config = new AclResourceGroup.FunctionConfig();
            BeanUtils.copyProperties(req.getConfig(), config);

            AclResourceGroup groupRequest = new AclResourceGroup();
            BeanUtils.copyProperties(req, groupRequest);
            groupRequest.setType(AclResourceGroup.TypeEnum.FUNCTION.getCode());
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
     * 保存功能项
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/saveItem")
    public ApiResponse<Void> saveItem(@Validated @RequestBody SaveFunctionItemRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("saveItem", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            Set<String> standardHttpMethodSet = ImmutableSet.copyOf(Arrays.stream(HttpMethod.values()).map(HttpMethod::name).toList());
            if(!CollectionUtils.isSubCollection(req.getHttpMethodList(), standardHttpMethodSet)){
                throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "存在非标准的HTTP请求方法");
            }

            AclResourceItem.Config config = new AclResourceItem.Config();
            BeanUtils.copyProperties(req.getConfig(), config);

            AclResourceItem itemRequest = new AclResourceItem();
            BeanUtils.copyProperties(req, itemRequest);
            itemRequest.setHttpMethodList(openPolicyAgentAuthorizationManager.httpMethodsToSortedString(req.getHttpMethodList()));
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
     * 移动功能行子树
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/move")
    public ApiResponse<Void> move(@Validated @RequestBody MoveFunctionRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("move", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            if(Objects.equals(ResourceGroupVo.RowTypeEnum.GROUP.getCode(), req.getRowType())){
                resourceGroupService.moveGroup(AclResourceGroup.TypeEnum.FUNCTION, req.getCode(), req.getMoveToGroupCode());
            }else{
                resourceGroupService.moveItem(req.getCode(), req.getMoveToGroupCode());
            }
            return null;
        });
    }

    /**
     * 删除功能行子树
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
                // 删除功能分组

                // 找出全部功能分组
                AclResourceGroup example = new AclResourceGroup();
                example.setType(AclResourceGroup.TypeEnum.FUNCTION.getCode());
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

                // 关联各个功能分组的子分组
                for (ResourceGroupVo groupVo : groupVoMap.values()) {
                    if(StringUtils.isNotBlank(groupVo.getParentGroupCode()) && !groupVo.getParentGroupCode().equals(groupVo.getCode())
                            && groupVoMap.containsKey(groupVo.getParentGroupCode())){
                        groupVoMap.get(groupVo.getParentGroupCode()).getResourceGroupVoList().add(groupVo);
                    }
                }

                // 查找待删除的功能分组树
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

                // 找出待删分组对应的功能项
                Specification<AclResourceItem> spec = (root, query, builder) -> {
                    List<Predicate> andPredicates = new ArrayList<>();
                    andPredicates.add(builder.in(root.get("parentGroupCode")).value(deleteGroupCodes));
                    return builder.and(andPredicates.toArray(new Predicate[0]));
                };
                List<String> deleteItemCodes = aclResourceItemRepository.findAll(spec).stream().map(AclResourceItem::getCode).toList();

                // 删除功能分组及对应的菜单项
                resourceGroupService.deleteGroupsAndItems(AclResourceGroup.TypeEnum.FUNCTION, deleteGroupCodes, deleteItemCodes);
            }else{
                // 删除功能项
                resourceGroupService.deleteGroupsAndItems(null, null, Lists.newArrayList(code));
            }
            return null;
        });
    }

    /**
     * 获取当前功能项的可选权限树列表
     * @param functionItemCode
     * @param context
     * @return
     */
    @PostMapping("/authority/listTree")
    public ApiResponse<List<ResourceAuthorityResponse>> listAuthorityTree(@RequestParam("functionItemCode") String functionItemCode, @CurrentSecurityContext SecurityContext context){
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

                // 获取功能项已关联的权限集合
                Set<String> grantedAuthorityCodes = aclAuthorityResourceRepository.findByResourceItemCodeIn(Lists.newArrayList(functionItemCode))
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
     * 功能项授权
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/authority/grant")
    public ApiResponse<Void> grantAuthority(@Validated @RequestBody FunctionGrantAuthorityRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("grantAuthority", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(!grantedAuthorities.contains(SystemRoleEnum.ROLE_ADMIN.name())){
                throw new BizException(ApiResponse.ErrCode.FORBIDDEN, "只有系统管理员才有权限操作");
            }

            resourceGroupService.grantResourceAuthorities(req.getFunctionItemCode(), req.getNewRoleCodeList(), req.getRemoveRoleCodeList());

            return null;
        });
    }

    /**
     * 判断当前登录用户，是否拥有指定的功能权限
     * @param req
     * @param bindingResult
     * @param context
     * @return
     */
    @PostMapping("/has")
    public ApiResponse<Boolean> has(@Validated @RequestBody HasFunctionRequest req, BindingResult bindingResult, @CurrentSecurityContext SecurityContext context){
        return super.apiCall("has", bindingResult, () -> {
            // 获取登录用户拥有的权限（包括层次权限）
            Set<String> grantedAuthorities = openPolicyAgentAuthorizationManager.getRoleHierarchy()
                    .getReachableGrantedAuthorities(context.getAuthentication().getAuthorities())
                    .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());

            if(CollectionUtils.isEmpty(grantedAuthorities)){
                return false;
            }

            // 获取指定资源项对应的权限编码集的映射
            Map<String, Set<String>> resourceItemToAuthoritiesMap = resourceGroupService.getResourceItemAuthoritiesUsingCache(req.getFunctionItemCodeList());
            if(MapUtils.isEmpty(resourceItemToAuthoritiesMap)){
                return false;
            }

            if(HasFunctionRequest.Type.ALL.getCode().equals(req.getType())){
                return resourceItemToAuthoritiesMap.values().stream().allMatch(authoritySet -> CollectionUtils.containsAny(authoritySet, grantedAuthorities));
            }

            if(HasFunctionRequest.Type.ANY.getCode().equals(req.getType())){
                return resourceItemToAuthoritiesMap.values().stream().anyMatch(authoritySet -> CollectionUtils.containsAny(authoritySet, grantedAuthorities));
            }

            return false;
        });
    }
}
