package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 基础应用的RocketMq消息发送服务
 * @Author: walter.tan
 * @DateTime: 2025-06-17 14:03:28
 */
@Slf4j
@Service
@ConditionalOnProperty(name = {"rocketmq.producer.group"})
public class InfraMessageRocketMqService extends AbstractInfraMessageService {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, String message) {
        rocketMQTemplate.convertAndSend(messageTopicEnum.name(), message);
        return null;
    }
}
