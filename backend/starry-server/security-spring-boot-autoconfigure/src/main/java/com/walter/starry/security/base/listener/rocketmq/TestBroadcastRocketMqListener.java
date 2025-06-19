package com.walter.starry.security.base.listener.rocketmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-19 15:31:48
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "#{T(com.walter.starry.security.base.common.enums.MessageTopicEnum).TEST_BROADCAST.name()}",
    consumerGroup = "${spring.application.name}", messageModel = MessageModel.BROADCASTING
)
public class TestBroadcastRocketMqListener implements RocketMQListener<String> {
    @Override
    public void onMessage(String message) {
        log.info("rocketmq TEST_BROADCAST message: {}", message);
    }
}
