package com.walter.starry.security.base.listener.pulsar;

import com.walter.starry.security.base.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

/**
 * 角色变更的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2024-03-27 15:30:34
 */
@Slf4j
@Component
public class RoleChangePulsarListener {
    @Autowired
    private RoleService roleService;

    @PulsarListener(
        topics = {"#{T(String).format(T(com.walter.starry.security.base.common.enums.MessageTopicEnum).ROLE_CHANGE_BROADCAST.getPulsarTopic(), @appPulsarProperties.baseReg.tenant, @appPulsarProperties.baseReg.namespace)}"},
        subscriptionName = "#{T(com.walter.starry.security.base.common.enums.MessageTopicEnum).ROLE_CHANGE_BROADCAST.name() + '-' + T(java.util.UUID).randomUUID()}",
        subscriptionType = SubscriptionType.Exclusive
    )
    public void listen(String message){
        System.out.println(">>>>>>" + message);
    }
}
