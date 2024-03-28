package com.walter.starry.security.base.service;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
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
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PulsarTemplate<String> stringPulsarTemplate;

    /**
     * 发送Redis广播消息（TODO 不建议使用Redis的PUB/SUB模式）
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
     * @return MessageId
     */
    public String publishToPulsar(MessageTopicEnum messageTopicEnum, String tenant, String namespace, String message) throws PulsarClientException {
        String topic = String.format(messageTopicEnum.getPulsarTopic(), tenant, namespace);
        MessageId messageId = stringPulsarTemplate.newMessage(message).withTopic(topic).send();
        return new String(messageId.toByteArray());
    }
}
