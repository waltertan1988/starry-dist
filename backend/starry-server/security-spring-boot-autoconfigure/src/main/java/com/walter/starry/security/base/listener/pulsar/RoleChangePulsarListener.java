package com.walter.starry.security.base.listener.pulsar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.service.RoleService;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色变更的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2024-03-27 15:30:34
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.pulsar.client.service-url")
public class RoleChangePulsarListener {
    @Autowired
    private RoleService roleService;

    @PulsarListener(
        topics = {"#{T(String).format(T(com.walter.starry.security.base.common.enums.MessageTopicEnum).ROLE_CHANGE_BROADCAST.getPulsarTopic(), @appPulsarProperties.baseReg.tenant, @appPulsarProperties.baseReg.namespace)}"},
        subscriptionName = "#{T(com.walter.starry.security.base.common.enums.MessageTopicEnum).ROLE_CHANGE_BROADCAST.name() + '-' + T(java.util.UUID).randomUUID()}",
        subscriptionType = SubscriptionType.Exclusive
    )
    public void listen(String message){
        log.info("Pulsar RoleChangeMessage message: {}", message);

        try{
            List<RoleChangeMessage> messageList = JsonUtil.toList(message, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
            roleService.tryRefreshLocalCaches(messageList);
        }catch (Exception ex){
            log.error("Pulsar RoleChangeMessage fail", ex);
        }
    }
}
