package com.walter.starry.security.base.listener.pulsar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资源变更的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
public class ResourceChangePulsarListener{
    @Autowired
    private ResourceGroupService resourceGroupService;

    @PulsarListener(
        topics = {"#{T(String).format(T(com.walter.starry.security.base.common.enums.MessageTopicEnum).RESOURCE_CHANGE_BROADCAST.getPulsarTopic(), @appPulsarProperties.baseReg.tenant, @appPulsarProperties.baseReg.namespace)}"},
        subscriptionName = "#{T(com.walter.starry.security.base.common.enums.MessageTopicEnum).RESOURCE_CHANGE_BROADCAST.name() + '-' + T(java.util.UUID).randomUUID()}",
        subscriptionType = SubscriptionType.Exclusive
    )
    public void listen(String message){
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
