package com.walter.starry.security.base.service;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.config.properties.AppPulsarProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-16 11:19:47
 */
@Slf4j
@Service
public class MessageService {
    @Autowired
    private AppPulsarProperties appPulsarProperties;
    @Autowired
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PulsarTemplate<String> stringPulsarTemplate;

    /**
     * 发送Redis广播消息（已废弃，建议使用Pulsar）
     * @param messageTopicEnum
     * @param message
     */
    @Deprecated
    public void publishBroadcastToRedis(MessageTopicEnum messageTopicEnum, String message){
        stringRedisTemplate.convertAndSend(messageTopicEnum.name(), Objects.requireNonNull(message));
    }

    /**
     * 发送Pulsar消息
     * @param messageTopicEnum
     * @param tenant
     * @param namespace
     * @param message
     * @return
     * @throws PulsarClientException
     */
    public String publishToPulsar(MessageTopicEnum messageTopicEnum, String tenant, String namespace, String message) throws PulsarClientException {
        String topic = String.format(messageTopicEnum.getPulsarTopic(), tenant, namespace);
        MessageId messageId = stringPulsarTemplate.newMessage(message).withTopic(topic).send();
        return messageId.toString();
    }

    /**
     * 发送Pulsar消息
     * @param messageTopicEnum
     * @param message
     * @return
     * @throws PulsarClientException
     */
    public String publishToPulsar(MessageTopicEnum messageTopicEnum, String message) throws PulsarClientException {
        return this.publishToPulsar(messageTopicEnum, appPulsarProperties.getBaseReg().getTenant(), appPulsarProperties.getBaseReg().getNamespace(), message);
    }
}
