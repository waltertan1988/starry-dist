package com.walter.starry.security.base.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.walter.starry.security.base.component.security.OpenPolicyAgentAuthorizationManager;
import com.walter.starry.security.base.entity.AclAuthorityResource;
import com.walter.starry.security.base.entity.AclResourceGroup;
import com.walter.starry.security.base.entity.AclResourceItem;
import com.walter.starry.security.base.repository.AclAuthorityResourceRepository;
import com.walter.starry.security.base.repository.AclResourceGroupRepository;
import com.walter.starry.security.base.repository.AclResourceItemRepository;
import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.common.concurrent.ExtendedVirtualThreadExecutorService;
import com.walter.starry.security.base.component.redis.InfraRedisKeys;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.exception.BizException;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.vo.response.resource.ResourceGroupVo;
import com.walter.starry.security.base.vo.response.resource.ResourceItemVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: walter.tan
 * @datetime: 2023/9/19 21:23
 */
@Slf4j
@Service
public class ResourceGroupService {
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;
    @Resource(name = "adminCommonVirtualThreadTaskExecutor")
    private ExtendedVirtualThreadExecutorService adminCommonVirtualThreadTaskExecutor;
    @Autowired
    private InfraRedisKeys infraRedisKeys;
    @Autowired
    private MessageService messageService;
    @Autowired
    private OpenPolicyAgentAuthorizationManager openPolicyAgentAuthorizationManager;
    @Autowired
    private AclResourceGroupRepository aclResourceGroupRepository;
    @Autowired
    private AclResourceItemRepository aclResourceItemRepository;
    @Autowired
    private AclAuthorityResourceRepository aclAuthorityResourceRepository;

    /**
     * 生成并获取指定类型的资源树
     * @param typeEnum 资源类型
     * @param rootResourceGroupCode 资源树所在的根分组
     * @param grantedAuthorities 只返回在此权限列表内的资源项
     * @return
     */
    public ResourceGroupVo generateResourceGroupTree(AclResourceGroup.TypeEnum typeEnum, String rootResourceGroupCode, Set<String> grantedAuthorities){
        // 查找指定权限集所拥有的资源项
        List<ResourceItemVo> resourceItemVoList = aclResourceItemRepository.findAuthorizedResourceItemByRoleAndResourceGroupType(typeEnum.getCode(), grantedAuthorities.stream().toList())
            .stream().map(item -> {
                ResourceItemVo vo = new ResourceItemVo();
                vo.setRowType(ResourceGroupVo.RowTypeEnum.ITEM.getCode());
                vo.setId(item.getId());
                vo.setCode(item.getCode());
                vo.setHttpMethodList(Lists.newArrayList(item.getHttpMethodList().split(OpenPolicyAgentAuthorizationManager.HTTP_METHOD_LIST_DELIMITER)));
                vo.setPattern(item.getPattern());
                vo.setName(item.getName());
                vo.setSeq(item.getSeq());
                vo.setParentGroupCode(item.getParentGroupCode());
                vo.setConfig(JsonUtil.toBean(item.getConfig(), AclResourceItem.Config.class));
                vo.setCreateTime(item.getCreateTime());
                vo.setUpdateTime(item.getUpdateTime());
                return vo;
            }).toList();

        // 回溯生成资源分组树
        return this.generateResourceGroupTree0(rootResourceGroupCode, resourceItemVoList, typeEnum);
    }

    /**
     * 回溯生成指定根分组的资源分组树（含各级分组和资源项）
     * @param rootGroupCode 根分组编码值
     * @param resourceItemVoList 回溯起点的资源项列表
     * @param typeEnum 资源类型
     * @return 根分组
     */
    private ResourceGroupVo generateResourceGroupTree0(String rootGroupCode, List<ResourceItemVo> resourceItemVoList, AclResourceGroup.TypeEnum typeEnum){
        ResourceGroupVo rootResourceGroupVo = null;

        // 查找所有资源分组
        final Map<String, ResourceGroupVo> allGroupVoMap = new HashMap<>();
        AclResourceGroup example = new AclResourceGroup();
        example.setType(typeEnum.getCode());
        for(AclResourceGroup aclResourceGroup : aclResourceGroupRepository.findAll(Example.of(example))) {
            ResourceGroupVo vo = new ResourceGroupVo();
            vo.setRowType(ResourceGroupVo.RowTypeEnum.GROUP.getCode());
            vo.setId(aclResourceGroup.getId());
            vo.setCode(aclResourceGroup.getCode());
            vo.setName(aclResourceGroup.getName());
            vo.setSeq(aclResourceGroup.getSeq());
            vo.setParentGroupCode(aclResourceGroup.getParentGroupCode());
            if(AclResourceGroup.TypeEnum.MENU.getCode().equals(typeEnum.getCode())){
                vo.setConfig(JsonUtil.toBean(aclResourceGroup.getConfig(), AclResourceGroup.MenuConfig.class));
            }else if(AclResourceGroup.TypeEnum.FUNCTION.getCode().equals(typeEnum.getCode())){
                vo.setConfig(JsonUtil.toBean(aclResourceGroup.getConfig(), AclResourceGroup.FunctionConfig.class));
            }
            vo.setCreateTime(aclResourceGroup.getCreateTime());
            vo.setUpdateTime(aclResourceGroup.getUpdateTime());
            vo.setResourceGroupVoList(new ArrayList<>());

            allGroupVoMap.put(vo.getCode(), vo);

            // 根分组
            if(Objects.equals(rootGroupCode, vo.getCode())){
                rootResourceGroupVo = vo;
            }
        }

        // 创建资源组编号与资源项列表的映射
        Map<String, List<ResourceItemVo>> groupToItemListMap = new HashMap<>();
        for (ResourceItemVo item : resourceItemVoList) {
            List<ResourceItemVo> list = groupToItemListMap.computeIfAbsent(item.getParentGroupCode(), key -> new ArrayList<>());
            list.add(item);
        }
        // 为资源项所在分组设置资源项列表
        for (String groupCode : groupToItemListMap.keySet()) {
            ResourceGroupVo groupVo = allGroupVoMap.get(groupCode);
            groupVo.getResourceGroupVoList().addAll(groupToItemListMap.get(groupCode));
        }

        // 回溯资源项所在分组
        this.generateResourceGroupTreeRecursively(allGroupVoMap, groupToItemListMap.keySet());

        // 从rootResourceGroupVo开始重新按层排序
        this.sortResourceGroupTree(rootResourceGroupVo);

        return rootResourceGroupVo;
    }

    /**
     * 对资源分组树进行按层排序
     * @param root
     */
    private void sortResourceGroupTree(ResourceGroupVo root){
        if(Objects.isNull(root)){
            return;
        }

        List<ResourceGroupVo> subGroupVoList = root.getResourceGroupVoList();

        if(CollectionUtils.isEmpty(subGroupVoList)){
            return;
        }

        // 按seq, id升序排序
        subGroupVoList.sort((a, b) -> {
            long seqCmp = a.getSeq() - b.getSeq();
            long idCmp = a.getId() - b.getId();
            return seqCmp > 0 ? 1 : (seqCmp < 0 ? -1 : (idCmp > 0 ? 1 : (idCmp < 0 ? -1 : 0)));
        });

        for (ResourceGroupVo resourceGroupVo : subGroupVoList) {
            this.sortResourceGroupTree(resourceGroupVo);
        }
    }

    /**
     * 从指定的资源子分组开始，递归回溯其父分组
     * @param allGroupVoMap 资源分组全集
     * @param fromGroupCodeSet 待回溯的资源子分组集合
     */
    private void generateResourceGroupTreeRecursively(Map<String, ResourceGroupVo> allGroupVoMap, Set<String> fromGroupCodeSet) {
        if(CollectionUtils.isEmpty(fromGroupCodeSet)){
            return;
        }

        final Set<String> nextFromGroupCodeSet = new HashSet<>();

        for (String fromGroupCode : fromGroupCodeSet) {
            ResourceGroupVo fromGroupVo = allGroupVoMap.get(fromGroupCode);
            String groupCode = fromGroupVo.getParentGroupCode();

            if(Objects.isNull(groupCode) || Objects.equals(fromGroupCode, groupCode)){
                // 已经遍历到根分组
                continue;
            }

            // 为子分组的父分组添加该子分组
            ResourceGroupVo groupVo = allGroupVoMap.get(groupCode);
            if(groupVo.getResourceGroupVoList().stream().map(ResourceGroupVo::getCode).noneMatch(fromGroupCode::equals)){
                groupVo.getResourceGroupVoList().add(fromGroupVo);

                nextFromGroupCodeSet.add(groupCode);
            }
        }

        // 递归处理
        this.generateResourceGroupTreeRecursively(allGroupVoMap, nextFromGroupCodeSet);
    }

    /**
     * 生成资源子树树根集合
     * @param resourceRows 待生成树根集合的原始资源行列表（包括资源分组和资源项）
     * @return
     */
    public List<ResourceGroupVo> getResourceTrees(UnmodifiableList<ResourceGroupVo> resourceRows) {
        List<ResourceGroupVo> resultList = new ArrayList<>();

        if(CollectionUtils.isEmpty(resourceRows)){
            return new ArrayList<>();
        }

        // 用于展示的树根资源编码集合
        final Set<String> displayRootCodes = new HashSet<>();

        Map<String, ResourceGroupVo> rowMap = new HashMap<>();
        for (ResourceGroupVo row : resourceRows) {
            ResourceGroupVo vo = row instanceof ResourceItemVo ? new ResourceItemVo() : new ResourceGroupVo();
            BeanUtils.copyProperties(row, vo);
            vo.setResourceGroupVoList(new ArrayList<>());

            rowMap.put(vo.getCode(), vo);
            resultList.add(vo);
        }

        // 排序
        resultList.sort(Comparator.comparing(ResourceGroupVo::getParentGroupCode)
                .thenComparingLong(ResourceGroupVo::getSeq)
                .thenComparingLong(ResourceGroupVo::getId));

        // 回溯每个资源行的父节点，作为其父节点的孩子
        for (ResourceGroupVo child : resultList) {
            if(Objects.isNull(child.getParentGroupCode()) || child.getParentGroupCode().equals(child.getCode())){
                // 当前资源行本身就是根
                displayRootCodes.add(child.getCode());
                continue;
            }

            ResourceGroupVo parent = rowMap.get(child.getParentGroupCode());
            if(Objects.nonNull(parent)){
                parent.getResourceGroupVoList().add(child);
            }else{
                displayRootCodes.add(child.getCode());
            }
        }

        // 在返回的结果列表中，剔除存在包含关系的子节点
        return resultList.stream().filter(o -> displayRootCodes.contains(o.getCode())).toList();
    }

    /**
     * 新建资源分组
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void createGroup(AclResourceGroup req) {
        Date now = new Date();
        req.setCreateTime(now);
        req.setUpdateTime(now);
        aclResourceGroupRepository.save(req);
    }

    /**
     * 更新资源分组
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(AclResourceGroup req) {
        AclResourceGroup group = aclResourceGroupRepository.findById(req.getId())
                .orElseThrow(() -> new BizException(ApiResponse.ErrCode.BAD_REQUEST, "找不到资源分组ID：" + req.getId()));
        Date now = new Date();
        String oldCode = group.getCode();

        // 更新分组信息
        group.setCode(req.getCode());
        group.setName(req.getName());
        group.setSeq(req.getSeq());
        group.setConfig(req.getConfig());
        group.setUpdateTime(now);
        aclResourceGroupRepository.save(group);

        // 更新关联的菜单项所属分组
        aclResourceItemRepository.updateParentGroupCode(oldCode, req.getCode(), now);
    }

    /**
     * 新增资源项
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void createItem(AclResourceItem req) {
        Date now = new Date();
        req.setCreateTime(now);
        req.setUpdateTime(now);
        aclResourceItemRepository.save(req);

        adminCommonVirtualThreadTaskExecutor.execute(() -> {
            // 新增的资源项，其顺序值可能会影响本地缓存里的排序
            ResourceChangeMessage.ResourceData after = new ResourceChangeMessage.ResourceData(req.getCode(), req.getName(), req.getHttpMethodList(), req.getPattern(), req.getSeq(), now);
            ResourceChangeMessage message = ResourceChangeMessage.ofCreate(after);
            messageService.publishBroadcastToRedis(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST, JsonUtil.toJson(Lists.newArrayList(message)));
        });
    }

    /**
     * 更新资源项
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(AclResourceItem req) {
        AclResourceItem item = aclResourceItemRepository.findById(req.getId())
                .orElseThrow(() -> new BizException(ApiResponse.ErrCode.BAD_REQUEST, "找不到资源项ID：" + req.getId()));

        AclResourceItem oldItem = new AclResourceItem();
        BeanUtils.copyProperties(item, oldItem);

        Date now = new Date();
        item.setCode(req.getCode());
        item.setName(req.getName());
        item.setHttpMethodList(req.getHttpMethodList());
        item.setPattern(req.getPattern());
        item.setSeq(req.getSeq());
        item.setConfig(req.getConfig());
        item.setUpdateTime(now);
        aclResourceItemRepository.save(item);

        if(!Objects.equals(item.getCode(), oldItem.getCode())
                || !Objects.equals(item.getHttpMethodList(), oldItem.getHttpMethodList())
                || !Objects.equals(item.getPattern(), oldItem.getPattern())
                || !Objects.equals(item.getSeq(), oldItem.getSeq())){
            adminCommonVirtualThreadTaskExecutor.execute(() -> {
                // 资源项编码、模式路径、请求方法类型或顺序发生变化，刷新本地缓存
                ResourceChangeMessage.ResourceData before = new ResourceChangeMessage.ResourceData(oldItem.getCode(), oldItem.getName(), oldItem.getHttpMethodList(), oldItem.getPattern(), oldItem.getSeq(), now);
                ResourceChangeMessage.ResourceData after = new ResourceChangeMessage.ResourceData(item.getCode(), item.getName(), item.getHttpMethodList(), item.getPattern(), item.getSeq(), now);
                ResourceChangeMessage message = ResourceChangeMessage.ofUpdate(before, after);
                messageService.publishBroadcastToRedis(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST, JsonUtil.toJson(Lists.newArrayList(message)));

                // 修改资源编码，需要剔除Redis缓存
                if(!Objects.equals(item.getCode(), oldItem.getCode())){
                    stringRedisTemplate.delete(infraRedisKeys.getResourceItemAuthoritiesKey(oldItem.getCode()));
                }
            });
        }
    }

    /**
     * 移动资源分组
     * @param typeEnum
     * @param code
     * @param moveToGroupCode
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveGroup(AclResourceGroup.TypeEnum typeEnum, String code, String moveToGroupCode) {
        if(Objects.equals(code, moveToGroupCode)){
            throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "请选择不同的分组");
        }

        AclResourceGroup example = new AclResourceGroup();
        example.setType(typeEnum.getCode());
        Map<String, AclResourceGroup> groupMap = aclResourceGroupRepository.findAll(Example.of(example))
                .stream().collect(Collectors.toMap(AclResourceGroup::getCode, Function.identity()));

        if(!groupMap.containsKey(code)){
            throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "找不到待移动分组:" + code);
        }

        if(!groupMap.containsKey(moveToGroupCode)){
            throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "找不到移动到分组:" + moveToGroupCode);
        }

        // 检查“移动到菜单分组编码”是否为“待移动菜单分组”的孩子
        boolean isMoveToGroupCodeChild4Code = false;
        AclResourceGroup currGroup = groupMap.get(moveToGroupCode);
        while (Objects.nonNull(currGroup)){
            if(Objects.equals(currGroup.getCode(), code)){
                isMoveToGroupCodeChild4Code = true;
                break;
            }

            if(StringUtils.isBlank(currGroup.getParentGroupCode()) || currGroup.getParentGroupCode().equals(currGroup.getCode())){
                // 已到达根分组
                break;
            }

            currGroup = groupMap.get(currGroup.getParentGroupCode());
        }

        if(isMoveToGroupCodeChild4Code){
            throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "不允许移动到子分组下面");
        }

        AclResourceGroup group = groupMap.get(code);
        group.setParentGroupCode(moveToGroupCode);
        group.setUpdateTime(new Date());
        aclResourceGroupRepository.save(group);
    }

    /**
     * 移动资源项
     * @param code
     * @param moveToGroupCode
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveItem(String code, String moveToGroupCode) {
        AclResourceItem example = new AclResourceItem();
        example.setCode(code);
        AclResourceItem item = aclResourceItemRepository.findOne(Example.of(example)).orElse(null);
        if(Objects.isNull(item)){
            throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "cannot find resource item:" + code);
        }
        item.setParentGroupCode(moveToGroupCode);
        item.setUpdateTime(new Date());
    }

    /**
     * 删除资源分组及资源项
     * @param typeEnum
     * @param groupCodes
     * @param itemCodes
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroupsAndItems(AclResourceGroup.TypeEnum typeEnum, List<String> groupCodes, List<String> itemCodes) {
        Date now = new Date();

        if(CollectionUtils.isNotEmpty(groupCodes)){
            // 删除资源分组
            Assert.notNull(typeEnum, "typeEnum cannot be null when deleting resource group");

            for (List<String> subList : Lists.partition(groupCodes, 50)) {
                aclResourceGroupRepository.deleteByCodes(typeEnum.getCode(), subList);
            }
        }

        if(CollectionUtils.isNotEmpty(itemCodes)){
            for (List<String> subList : Lists.partition(itemCodes, 50)) {
                // 删除资源项
                aclResourceItemRepository.deleteByCodes(subList);
                // 删除资源与权限的关联
                aclAuthorityResourceRepository.deleteByResourceItemCode(subList);
            }

            adminCommonVirtualThreadTaskExecutor.execute(() -> {
                // 刷新本地缓存
                List<ResourceChangeMessage> messageList = itemCodes.stream()
                    .map(code -> ResourceChangeMessage.ofDelete(
                        new ResourceChangeMessage.ResourceData(code, null, null, null, null, now)
                    )).toList();
                messageService.publishBroadcastToRedis(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST, JsonUtil.toJson(messageList));

                // 剔除Redis缓存
                for (String itemCode : itemCodes) {
                    stringRedisTemplate.delete(infraRedisKeys.getResourceItemAuthoritiesKey(itemCode));
                }
            });
        }
    }

    /**
     * 资源授权
     * @param resourceItemCode 资源项编码
     * @param newRoleCodeList 待添加的角色集合
     * @param removeRoleCodeList 待移除的角色集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void grantResourceAuthorities(String resourceItemCode, List<String> newRoleCodeList, List<String> removeRoleCodeList) {
        if(StringUtils.isBlank(resourceItemCode)){
            throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "资源项编码为空");
        }

        if(CollectionUtils.isEmpty(newRoleCodeList) && CollectionUtils.isEmpty(removeRoleCodeList)){
            throw new BizException(ApiResponse.ErrCode.BAD_REQUEST, "待添加角色集合和待移除角色集合不能同时为空");
        }

        Date now = new Date();

        if(CollectionUtils.isNotEmpty(newRoleCodeList)){
            List<AclAuthorityResource> insertList = newRoleCodeList.stream().map(code -> {
                AclAuthorityResource insert = new AclAuthorityResource();
                insert.setResourceItemCode(resourceItemCode);
                insert.setAuthorityItemCode(code);
                insert.setCreateTime(now);
                insert.setUpdateTime(now);
                return insert;
            }).toList();
            aclAuthorityResourceRepository.saveAll(insertList);
        }

        if(CollectionUtils.isNotEmpty(removeRoleCodeList)){
            aclAuthorityResourceRepository.deleteByResourceItemCodeAndAuthorityItemCodes(resourceItemCode, removeRoleCodeList);
        }

        adminCommonVirtualThreadTaskExecutor.execute(() -> {
            // 刷新本地缓存
            ResourceChangeMessage.ChangeAuthorityData changeAuthorityData = new ResourceChangeMessage.ChangeAuthorityData(
                    resourceItemCode, newRoleCodeList, removeRoleCodeList);
            ResourceChangeMessage message = ResourceChangeMessage.ofChangeAuthority(now, changeAuthorityData);
            messageService.publishBroadcastToRedis(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST, JsonUtil.toJson(Lists.newArrayList(message)));

            // 剔除Redis缓存
            stringRedisTemplate.delete(infraRedisKeys.getResourceItemAuthoritiesKey(resourceItemCode));
        });
    }

    /**
     * 结合缓存获取指定资源项对应的权限集合的映射
     * @param resourceItemCodeList
     * @return
     */
    public Map<String, Set<String>> getResourceItemAuthoritiesUsingCache(List<String> resourceItemCodeList) {
        Assert.notEmpty(resourceItemCodeList, "resourceItemCodeList cannot be empty");

        Map<String, Set<String>> resultMap = new HashMap<>();

        // 优先查找缓存（并发）
        List<String> notCachedResourceItemCodeList = new ArrayList<>();
        List<CompletableFuture<Void>> completableFutureList = resourceItemCodeList.stream().map(resItemCode -> CompletableFuture.runAsync(() -> {
            String key = infraRedisKeys.getResourceItemAuthoritiesKey(resItemCode);
            String authorityArrayJson = stringRedisTemplate.opsForValue().get(key);

            if(StringUtils.isBlank(authorityArrayJson)){
                // 尚未建立缓存
                notCachedResourceItemCodeList.add(resItemCode);
            }else{
                // 已建立缓存
                List<String> authorityList = JsonUtil.toList(authorityArrayJson, new TypeReference<>() {});
                if(CollectionUtils.isNotEmpty(authorityList)){
                    resultMap.put(resItemCode, Sets.newHashSet(authorityList));
                }
            }
        }, adminCommonVirtualThreadTaskExecutor)).toList();
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();

        if(CollectionUtils.isEmpty(notCachedResourceItemCodeList)){
            // 所有功能项已在缓存里命中，直接返回结果集
            return resultMap;
        }

        // 未命中的缓存，重新查库并写入结果集
        Map<String, List<AclAuthorityResource>> notCachedResourceItemCodeMap = aclAuthorityResourceRepository.findByResourceItemCodeIn(notCachedResourceItemCodeList)
                .stream().collect(Collectors.groupingBy(AclAuthorityResource::getResourceItemCode));

        for (String notCacheResItemCode : notCachedResourceItemCodeList) {
            resultMap.put(notCacheResItemCode, notCachedResourceItemCodeMap.getOrDefault(notCacheResItemCode, new ArrayList<>()).stream().map(AclAuthorityResource::getAuthorityItemCode).collect(Collectors.toSet()));
        }

        try{
            // 对未命中的缓存进行重新构建
            completableFutureList = notCachedResourceItemCodeList.stream().map(notCachedResourceItemCode -> CompletableFuture.runAsync(() -> {
                List<String> authorityList = notCachedResourceItemCodeMap.getOrDefault(notCachedResourceItemCode, new ArrayList<>())
                        .stream().map(AclAuthorityResource::getAuthorityItemCode).toList();

                String key = infraRedisKeys.getResourceItemAuthoritiesKey(notCachedResourceItemCode);
                stringRedisTemplate.opsForValue().set(key, Objects.requireNonNull(JsonUtil.toJson(authorityList)), 1, TimeUnit.HOURS);
            }, adminCommonVirtualThreadTaskExecutor)).toList();
            CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
        }catch (Exception ex){
            log.error("getResourceItemAuthoritiesUsingCache rebuild cache fail. notCachedResourceItemCodeList:{}", JsonUtil.toJson(notCachedResourceItemCodeList), ex);
        }

        return resultMap;
    }

    /**
     * 检查并尝试刷新本地缓存（资源与权限的关联关系）
     * @param messageList
     */
    public void tryRefreshLocalCaches(List<ResourceChangeMessage> messageList) {
        boolean needRefreshRequestMatcherEntryHolder = false;

        for (ResourceChangeMessage changeMessage : messageList) {
            if(Objects.nonNull(changeMessage.getChangeAuthorityData())){
                // 资源权限变更
                needRefreshRequestMatcherEntryHolder = true;
                break;
            }else if(Objects.nonNull(changeMessage.getBefore()) && Objects.nonNull(changeMessage.getAfter())){
                // 修改资源

                if(!Objects.equals(changeMessage.getAfter().getCode(), changeMessage.getBefore().getCode())){
                    // 修改资源编码
                    needRefreshRequestMatcherEntryHolder = true;
                    break;
                }

                if(!Objects.equals(changeMessage.getAfter().getPattern(), changeMessage.getBefore().getPattern())){
                    // 修改路径模式
                    needRefreshRequestMatcherEntryHolder = true;
                    break;
                }

                if(StringUtils.isNotBlank(changeMessage.getAfter().getHttpMethodList()) && StringUtils.isNotBlank(changeMessage.getBefore().getHttpMethodList())){
                    Collection<String> beforeHttpMethods = Lists.newArrayList(changeMessage.getBefore().getHttpMethodList().split(OpenPolicyAgentAuthorizationManager.HTTP_METHOD_LIST_DELIMITER));
                    Collection<String> afterHttpMethods = Lists.newArrayList(changeMessage.getAfter().getHttpMethodList().split(OpenPolicyAgentAuthorizationManager.HTTP_METHOD_LIST_DELIMITER));
                    if(!CollectionUtils.isEqualCollection(beforeHttpMethods, afterHttpMethods)){
                        // 修改http请求方法类型
                        needRefreshRequestMatcherEntryHolder = true;
                        break;
                    }
                }

                if(!Objects.equals(changeMessage.getAfter().getSeq(), changeMessage.getBefore().getSeq())){
                    // 修改顺序
                    needRefreshRequestMatcherEntryHolder = true;
                    break;
                }
            }else if(Objects.isNull(changeMessage.getBefore())){
                // 新增资源
                needRefreshRequestMatcherEntryHolder = true;
                break;
            }else {
                // 删除资源
                needRefreshRequestMatcherEntryHolder = true;
                break;
            }
        }

        if(needRefreshRequestMatcherEntryHolder){
            openPolicyAgentAuthorizationManager.refreshRequestMatcherEntryHolder();
        }
    }
}
