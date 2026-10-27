package com.walter.starry.security.base.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.common.constant.MqConstant;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基础广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2026-10-25 13:43:50
 */
@Slf4j
@Component
@ConditionalOnProperty(name = MqConstant.Kafka.CONDITIONAL_ON_PROP)
public class InfraKafkaBroadcastListener {
    @Autowired
    private RoleService roleService;
    @Autowired
    private ResourceGroupService resourceGroupService;

    @KafkaListener(
            id = "#{T(com.walter.starry.common.constant.MqConstant.Kafka).INFRA_BROADCAST_TOPIC.formatted('${spring.application.name}') + '.' + T(com.walter.starry.common.util.IdUtil).getUUID()}",
            topics = "#{T(com.walter.starry.common.constant.MqConstant.Kafka).INFRA_BROADCAST_TOPIC.formatted('${spring.application.name}')}",
            concurrency = "1",
            containerFactory = MqConstant.Kafka.BEAN_NAME_INFRA_SHARE_KAFKA_LISTENER_CONTAINER_FACTORY)
    public void handle(ConsumerRecord<String, String> record) {
        log.info("kafka INFRA_BROADCAST_TOPIC message received. record: {}", record.toString());

        String message = record.value();
        List<String> msg = JsonUtil.toList(message, new TypeReference<>() {});
        String topic = msg.getFirst();
        String payload = msg.getLast();

        if(MessageTopicEnum.ROLE_CHANGE_BROADCAST.getKafka().getTopic().equals(topic)){
            List<RoleChangeMessage> messageList = JsonUtil.toList(payload, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
            roleService.tryRefreshLocalCaches(messageList);
        }else if(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST.getKafka().getTopic().equals(topic)){
            List<ResourceChangeMessage> messageList = JsonUtil.toList(payload, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（资源与权限的关联关系）
            resourceGroupService.tryRefreshLocalCaches(messageList);
        }
    }
}
