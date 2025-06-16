package com.walter.starry.security.base.service;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.config.properties.AppMsgProps;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.MessageId;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-16 11:19:47
 */
@Slf4j
@Service
public class MessageService {
    @Autowired
    private AppMsgProps appMsgProps;
    @Autowired
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private PulsarTemplate<String> stringPulsarTemplate;

    /**
     * 发送本应用的广播消息消息
     * @param messageTopicEnum
     * @param message
     * @return
     */
    @Nullable
    public String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, String message) {
        if(appMsgProps.getRedis().isEnabled()){
            Long receivedClientNum = this.publishBroadcastToRedis(messageTopicEnum, message);
            return Objects.toString(receivedClientNum, null);
        }

        if(appMsgProps.getRocketMq().isEnabled()){
            this.publishToRocketMq(messageTopicEnum, message);
            return null;
        }

        if (Objects.nonNull(appMsgProps.getPulsar().getBaseReg()) && appMsgProps.getPulsar().getBaseReg().enabled()) {
            return this.publishToPulsar(messageTopicEnum, appMsgProps.getPulsar().getBaseReg().getTenant(), appMsgProps.getPulsar().getBaseReg().getNamespace(), message);
        }

        throw new RuntimeException("sendBroadcastMessage not support");
    }

    /**
     * 发送Redis广播消息
     * @param messageTopicEnum
     * @param message
     */
    public Long publishBroadcastToRedis(MessageTopicEnum messageTopicEnum, String message){
        return stringRedisTemplate.convertAndSend(messageTopicEnum.name(), Objects.requireNonNull(message));
    }

    /**
     * 发送Pulsar消息
     * @param messageTopicEnum
     * @param tenant
     * @param namespace
     * @param message
     * @return
     */
    public String publishToPulsar(MessageTopicEnum messageTopicEnum, String tenant, String namespace, String message) {
        String topic = String.format(messageTopicEnum.getPulsar().getTopic(), tenant, namespace);
        MessageId messageId = stringPulsarTemplate.newMessage(message).withTopic(topic).send();
        return messageId.toString();
    }

    /**
     * 发送RocketMq消息
     * @param messageTopicEnum
     * @param message
     */
    public void publishToRocketMq(MessageTopicEnum messageTopicEnum, String message){
        rocketMQTemplate.convertAndSend(messageTopicEnum.name(), message);
    }
}
