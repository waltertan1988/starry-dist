package com.walter.starry.security.base.listener.rocketmq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.service.msg.InfraMessageRocketMqService;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资源变更的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "RESOURCE_CHANGE_BROADCAST",
    consumerGroup = "${spring.application.name}", messageModel = MessageModel.BROADCASTING
)
@ConditionalOnBean(InfraMessageRocketMqService.class)
public class ResourceChangeRocketMqListener implements RocketMQListener<String> {
    @Autowired
    private ResourceGroupService resourceGroupService;

    @Override
    public void onMessage(String body) {
        log.info("rocketmq ResourceChangeMessage body: {}", body);
        List<ResourceChangeMessage> messageList = JsonUtil.toList(body, new TypeReference<>() {});

        // 检查并尝试刷新本地缓存（资源与权限的关联关系）
        resourceGroupService.tryRefreshLocalCaches(messageList);
    }
}
