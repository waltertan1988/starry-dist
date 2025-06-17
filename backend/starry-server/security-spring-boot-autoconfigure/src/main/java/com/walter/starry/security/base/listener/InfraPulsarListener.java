package com.walter.starry.security.base.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.component.pulsar.PulsarTopicConfig;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.service.RoleService;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 监听应用的基础Pulsar消息
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
@ConditionalOnBean(PulsarTopicConfig.class)
public class InfraPulsarListener {
    @Autowired
    private RoleService roleService;
    @Autowired
    private ResourceGroupService resourceGroupService;

    /**
     * 角色变更的广播消息订阅
     * @param message
     */
    @PulsarListener(
        topics = {"#{T(String).format(T(com.walter.starry.security.base.common.enums.MessageTopicEnum).ROLE_CHANGE_BROADCAST.pulsar.topic, @appMsgProps.pulsar.baseReg.tenant, @appMsgProps.pulsar.baseReg.namespace)}"},
        subscriptionName = "#{T(com.walter.starry.security.base.common.enums.MessageTopicEnum).ROLE_CHANGE_BROADCAST.name() + '-' + T(java.util.UUID).randomUUID()}",
        subscriptionType = SubscriptionType.Exclusive
    )
    public void onRoleChange(String message){
        log.info("pulsar RoleChangeMessage message: {}", message);

        try{
            List<RoleChangeMessage> messageList = JsonUtil.toList(message, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
            roleService.tryRefreshLocalCaches(messageList);
        }catch (Exception ex){
            log.error("pulsar RoleChangeMessage fail", ex);
        }
    }

    /**
     * 资源变更的广播消息订阅
     * @param message
     */
    @PulsarListener(
        topics = {"#{T(String).format(T(com.walter.starry.security.base.common.enums.MessageTopicEnum).RESOURCE_CHANGE_BROADCAST.pulsar.topic, @appMsgProps.pulsar.baseReg.tenant, @appMsgProps.pulsar.baseReg.namespace)}"},
        subscriptionName = "#{T(com.walter.starry.security.base.common.enums.MessageTopicEnum).RESOURCE_CHANGE_BROADCAST.name() + '-' + T(java.util.UUID).randomUUID()}",
        subscriptionType = SubscriptionType.Exclusive
    )
    public void onResourceChange(String message){
        log.info("Pulsar ResourceChangeMessage message: {}", message);

        try{
            List<ResourceChangeMessage> messageList = JsonUtil.toList(message, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（资源与权限的关联关系）
            resourceGroupService.tryRefreshLocalCaches(messageList);
        }catch (Exception ex){
            log.error("Pulsar ResourceChangeMessage fail", ex);
        }
    }
}
