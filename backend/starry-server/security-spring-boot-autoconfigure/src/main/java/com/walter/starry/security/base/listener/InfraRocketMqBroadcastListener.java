package com.walter.starry.security.base.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.service.RoleService;
import com.walter.starry.security.base.service.msg.InfraMessageRocketMqService;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-19 15:31:48
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = InfraRocketMqBroadcastListener.TOPIC, namespace = "${rocketmq.producer.namespace}",
    consumerGroup = "${spring.application.name}", messageModel = MessageModel.BROADCASTING
)
@ConditionalOnBean(InfraMessageRocketMqService.class)
public class InfraRocketMqBroadcastListener implements RocketMQListener<MessageExt> {
    public static final String TOPIC = "INFRA_BROADCAST";
    @Autowired
    private RoleService roleService;
    @Autowired
    private ResourceGroupService resourceGroupService;

    @Override
    public void onMessage(MessageExt messageExt) {
        String tags = messageExt.getTags();
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("rocketmq messageExt received. tags: {}, body: {}", tags, body);

        if(MessageTopicEnum.ROLE_CHANGE_BROADCAST.getRocketMq().getTags().equals(tags)){
            List<RoleChangeMessage> messageList = JsonUtil.toList(body, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
            roleService.tryRefreshLocalCaches(messageList);
        }else if(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST.getRocketMq().getTags().equals(tags)){
            List<ResourceChangeMessage> messageList = JsonUtil.toList(body, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（资源与权限的关联关系）
            resourceGroupService.tryRefreshLocalCaches(messageList);
        }
    }
}
