package com.walter.starry.security.base.listener.rocketmq;

import com.walter.starry.security.base.service.msg.InfraMessageRocketMqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-19 15:31:48
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "TEST_BROADCAST",
    consumerGroup = "${spring.application.name}", messageModel = MessageModel.BROADCASTING
)
@ConditionalOnBean(InfraMessageRocketMqService.class)
public class TestBroadcastRocketMqListener implements RocketMQListener<String> {
    @Override
    public void onMessage(String message) {
        log.info("rocketmq TEST_BROADCAST message: {}", message);
    }
}
