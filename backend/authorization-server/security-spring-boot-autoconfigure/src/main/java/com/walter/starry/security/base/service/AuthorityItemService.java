package com.walter.starry.security.base.service;

import com.google.common.collect.Lists;
import com.walter.starry.security.base.bo.AuthorityItemBo;
import com.walter.starry.security.base.component.security.JpaUserDetailsService;
import com.walter.starry.security.base.entity.AclAuthorityItem;
import com.walter.starry.security.base.repository.AclAuthorityItemRepository;
import com.walter.starry.security.base.repository.AclAuthorityRepository;
import com.walter.starry.security.base.repository.AclAuthorityResourceRepository;
import com.walter.starry.security.base.vo.request.role.MoveRoleRequest;
import com.walter.starry.security.base.vo.request.role.SaveRoleRequest;
import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.common.concurrent.ExtendedVirtualThreadExecutorService;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.exception.BizException;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.util.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: walter.tan
 * @datetime: 2023/9/19 21:23
 */
@Slf4j
@Service
public class AuthorityItemService {
    @Autowired
    private AclAuthorityItemRepository aclAuthorityItemRepository;
    @Autowired
    private AclAuthorityRepository aclAuthorityRepository;
    @Autowired
    private AclAuthorityResourceRepository aclAuthorityResourceRepository;
    @Autowired
    private JpaUserDetailsService jpaUserDetailsService;
    @Autowired
    private MessageService messageService;
    @Resource(name = "adminCommonVirtualThreadTaskExecutor")
    private ExtendedVirtualThreadExecutorService adminCommonVirtualThreadTaskExecutor;

    /**
     * 获取所有完整且已排序的权限树列表（即权限森林）
     * @param clz 权限树节点类型
     * @param postConsumer 自定义的后处理，参数为：全部权限的map
     * @return
     */
    public <T extends AuthorityItemBo> List<T> getAllAuthorityTrees(Class<T> clz, Consumer<Map<String, T>> postConsumer) throws Exception {
        // 查询全部权限
        Map<String, T> allItemBoMap = new HashMap<>();
        for (AclAuthorityItem item : aclAuthorityItemRepository.findAll()) {
            T bo = clz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(item, bo);
            bo.setChildren(new ArrayList<>());
            allItemBoMap.put(bo.getCode(), bo);
        }

        // 构造权限森林
        List<T> rootAuthorityList = new ArrayList<>();
        for (T bo : allItemBoMap.values()) {
            if(StringUtils.isBlank(bo.getParentCode()) || bo.getParentCode().equals(bo.getCode())){
                // 根权限
                rootAuthorityList.add(bo);
                continue;
            }

            T parentBo = allItemBoMap.get(bo.getParentCode());
            parentBo.getChildren().add(bo);
        }

        // 对每棵权限树各层子节点列表进行排序
        Queue<List<T>> sortingQueue = new LinkedList<>();
        sortingQueue.offer(rootAuthorityList);
        while(!sortingQueue.isEmpty()){
            List<T> head = sortingQueue.poll();
            head.sort(Comparator.comparingInt(T::getPriority).thenComparingLong(T::getId));

            for (T child : head) {
                if(CollectionUtils.isNotEmpty(child.getChildren())){
                    sortingQueue.offer(new ArrayList<>(child.getChildren().stream().map(clz::cast).toList()));
                }
            }
        }

        // 执行自定义的后处理
        if(Objects.nonNull(postConsumer)){
            postConsumer.accept(allItemBoMap);
        }

        return rootAuthorityList;
    }

    /**
     * 根据入参的权限列表，生成对应的权限树根列表（即权限森林）
     * @param clz
     * @param authorityItemList
     * @return
     * @param <T>
     */
    public <T extends AuthorityItemBo> List<T> getAuthorityTrees(Class<T> clz, List<AclAuthorityItem> authorityItemList) throws Exception{
        List<T> resultList = new ArrayList<>();

        if(CollectionUtils.isEmpty(authorityItemList)){
            return resultList;
        }

        // 用于展示的树根权限编码集合
        final Set<String> displayRootCodes = new HashSet<>();

        Map<String, T> authorityItemBoMap = new HashMap<>();
        for (AclAuthorityItem item : authorityItemList) {
            T bo = clz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(item, bo);
            bo.setChildren(new ArrayList<>());

            authorityItemBoMap.put(bo.getCode(), bo);
            resultList.add(bo);
        }

        // 回溯每个权限的父节点，作为其父节点的孩子
        for (T child : resultList) {
            if(Objects.isNull(child.getParentCode()) || child.getParentCode().equals(child.getCode())){
                // 当前权限本身就是根权限
                displayRootCodes.add(child.getCode());
                continue;
            }

            T parent = authorityItemBoMap.get(child.getParentCode());
            if(Objects.nonNull(parent)){
                parent.getChildren().add(child);
            }else{
                displayRootCodes.add(child.getCode());
            }
        }

        // 在返回的结果列表中，剔除存在包含关系的子节点
        return resultList.stream().filter(o -> displayRootCodes.contains(o.getCode())).toList();
    }

    /**
     * 新增角色
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SaveRoleRequest req) {
        Date now = new Date();
        AclAuthorityItem aclAuthorityItem = new AclAuthorityItem();
        BeanUtils.copyProperties(req, aclAuthorityItem);
        aclAuthorityItem.setCreateTime(now);
        aclAuthorityItem.setUpdateTime(now);
        aclAuthorityItemRepository.save(aclAuthorityItem);

        adminCommonVirtualThreadTaskExecutor.execute(() -> {
            // 发送“角色变更”广播，刷新层次角色的本地缓存
            RoleChangeMessage message = RoleChangeMessage.ofCreate(new RoleChangeMessage.RoleData(req.getCode(), req.getName(), req.getParentCode(), now));
            messageService.publishBroadcastToRedis(MessageTopicEnum.ROLE_CHANGE_BROADCAST, JsonUtil.toJson(Lists.newArrayList(message)));
        });
    }

    /**
     * 更新角色
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SaveRoleRequest req) {
        Date now = new Date();
        final AclAuthorityItem oldAclAuthorityItem = new AclAuthorityItem();

        AclAuthorityItem aclAuthorityItem = aclAuthorityItemRepository.findById(req.getId())
                .orElseThrow(() -> new BizException(ApiResponse.ErrCode.BAD_REQUEST, "找不到角色ID：" + req.getId()));
        // 备份角色原来的信息
        BeanUtils.copyProperties(aclAuthorityItem, oldAclAuthorityItem);

        // 更新角色数据
        BeanUtils.copyProperties(req, aclAuthorityItem);
        aclAuthorityItem.setUpdateTime(now);

        if(!Objects.equals(req.getCode(), oldAclAuthorityItem.getCode())){
            // 修改了角色编码的情况

            // 更新下级角色的父角色编码
            aclAuthorityItemRepository.updateParentCode(oldAclAuthorityItem.getCode(), req.getCode(), now);
            // 重新绑定角色与用户的关系
            aclAuthorityRepository.updateAuthority(oldAclAuthorityItem.getCode(), req.getCode(), now);
            // 重新绑定角色与资源的关系
            aclAuthorityResourceRepository.updateAuthorityItemCode(oldAclAuthorityItem.getCode(), req.getCode(), now);
        }

        adminCommonVirtualThreadTaskExecutor.execute(() -> {
            // 发送“角色变更”广播，刷新层次角色的本地缓存
            RoleChangeMessage.RoleData before = new RoleChangeMessage.RoleData(oldAclAuthorityItem.getCode(), oldAclAuthorityItem.getName(), oldAclAuthorityItem.getParentCode(), now);
            RoleChangeMessage.RoleData after = new RoleChangeMessage.RoleData(req.getCode(), req.getName(), req.getParentCode(), now);
            messageService.publishBroadcastToRedis(MessageTopicEnum.ROLE_CHANGE_BROADCAST, JsonUtil.toJson(Lists.newArrayList(RoleChangeMessage.ofUpdate(before, after))));

            // 踢出受角色编码变更影响的用户的session
            if(!Objects.equals(req.getCode(), oldAclAuthorityItem.getCode())){
                jpaUserDetailsService.removeSession(Lists.newArrayList(req.getCode(), oldAclAuthorityItem.getCode()));
            }
        });
    }

    /**
     * 删除角色
     * @param codeList
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<String> codeList) {
        if(CollectionUtils.isEmpty(codeList)){
            return;
        }

        Date now  = new Date();

        for (List<String> codes : Lists.partition(codeList, 100)) {
            // 删除角色
            aclAuthorityItemRepository.deleteByCodes(codes);
            // 解除角色与用户的关系
            aclAuthorityRepository.deleteByAuthorities(codes);
            // 解除角色与资源的关系
            aclAuthorityResourceRepository.deleteByAuthorityItemCodes(codes);
        }

        adminCommonVirtualThreadTaskExecutor.execute(() -> {
            // 发送“角色变更”广播，刷新层次角色及权限与资源关联关系的本地缓存
            List<RoleChangeMessage> messageList = codeList.stream()
                    .map(code -> RoleChangeMessage.ofDelete(new RoleChangeMessage.RoleData(code, null, null, now)))
                    .toList();
            messageService.publishBroadcastToRedis(MessageTopicEnum.ROLE_CHANGE_BROADCAST, JsonUtil.toJson(messageList));

            // 踢出待删角色对应用户的session
            jpaUserDetailsService.removeSession(codeList);
        });
    }

    /**
     * 移动角色
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void move(MoveRoleRequest req) {
        Date now = new Date();
        AclAuthorityItem example = new AclAuthorityItem();
        example.setCode(req.getCode());
        AclAuthorityItem aclAuthorityItem = aclAuthorityItemRepository.findOne(Example.of(example))
                .orElseThrow(() -> new BizException(ApiResponse.ErrCode.BAD_REQUEST, "找不到角色编码：" + req.getCode()));

        final String oldParentCode = aclAuthorityItem.getParentCode();
        if(Objects.equals(req.getMoveToCode(), oldParentCode)){
            return;
        }

        aclAuthorityItem.setParentCode(req.getMoveToCode());
        aclAuthorityItem.setUpdateTime(now);

        adminCommonVirtualThreadTaskExecutor.execute(() -> {
            // 发送“角色变更”广播，刷新层次角色的本地缓存
            RoleChangeMessage.RoleData before = new RoleChangeMessage.RoleData(req.getCode(), null, oldParentCode, now);
            RoleChangeMessage.RoleData after = new RoleChangeMessage.RoleData(req.getCode(), null, req.getMoveToCode(), now);
            messageService.publishBroadcastToRedis(MessageTopicEnum.ROLE_CHANGE_BROADCAST, JsonUtil.toJson(Lists.newArrayList(RoleChangeMessage.ofUpdate(before, after))));
        });
    }

    /**
     * 获取指定权限编码的全部祖先的权限编码
     * @param authorityItemCode
     */
    public Set<String> getAncestorAuthorityItemCodes(String authorityItemCode){
        Set<String> resultSet = new HashSet<>();

        Map<String, AclAuthorityItem> authorityItemMap = aclAuthorityItemRepository.findAll()
                .stream().collect(Collectors.toUnmodifiableMap(AclAuthorityItem::getCode, Function.identity()));

        AclAuthorityItem current = authorityItemMap.get(authorityItemCode);

        // 当前权限节点不存在，或当前权限节点本身就是根节点
        if(Objects.isNull(current)
                || StringUtils.isBlank(current.getParentCode()) || current.getParentCode().equals(current.getCode())){
            return resultSet;
        }

        AclAuthorityItem parent = authorityItemMap.get(current.getParentCode());
        while (Objects.nonNull(parent) && !parent.getCode().equals(current.getCode())){
            resultSet.add(parent.getCode());

            current = parent;
            if(StringUtils.isNotBlank(current.getParentCode())){
                parent = authorityItemMap.get(current.getParentCode());
            }
        }

        return resultSet;
    }
}
