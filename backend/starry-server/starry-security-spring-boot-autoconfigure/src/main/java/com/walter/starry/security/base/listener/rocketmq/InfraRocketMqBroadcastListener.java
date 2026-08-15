package com.walter.starry.security.base.listener.rocketmq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.service.RoleService;
import com.walter.starry.security.base.service.msg.InfraMessageRocketMqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
@ConditionalOnProperty(name = InfraMessageRocketMqService.CONDITIONAL_ON_PROPERTIES_NAME)
public class InfraRocketMqBroadcastListener extends AbstractRocketMqMessageListener {

    public static final String TOPIC = "INFRA_BROADCAST";

    @Autowired
    private RoleService roleService;
    @Autowired
    private ResourceGroupService resourceGroupService;

    @Override
    public void handle(MessageExt messageExt) {
        String tags = messageExt.getTags();
        String message = new String(messageExt.getBody());

        log.info("rocketmq messageExt received. tags: {}, msgId: {}, message: {}", tags, messageExt.getMsgId(), message);

        if(MessageTopicEnum.ROLE_CHANGE_BROADCAST.getRocketMq().getTags().equals(tags)){
            List<RoleChangeMessage> messageList = JsonUtil.toList(message, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
            roleService.tryRefreshLocalCaches(messageList);
        }else if(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST.getRocketMq().getTags().equals(tags)){
            List<ResourceChangeMessage> messageList = JsonUtil.toList(message, new TypeReference<>() {});

            // 检查并尝试刷新本地缓存（资源与权限的关联关系）
            resourceGroupService.tryRefreshLocalCaches(messageList);
        }
    }
}
