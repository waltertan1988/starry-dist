package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.MessageId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

/**
 * 基础应用的Pulsar消息发送服务
 * @Author: walter.tan
 * @DateTime: 2025-06-17 14:03:28
 */
@Slf4j
@Service
@ConditionalOnProperty(name = {"spring.pulsar.client.service-url", "spring.pulsar.admin.service-url"})
public class InfraMessagePulsarService extends AbstractInfraMessageService {
    @Autowired
    private PulsarTemplate<String> stringPulsarTemplate;

    @Override
    public String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, String message) {
        return this.publishToPulsar(messageTopicEnum, appMsgProps.getPulsar().getBaseReg().getTenant(), appMsgProps.getPulsar().getBaseReg().getNamespace(), message);
    }

    public String publishToPulsar(MessageTopicEnum messageTopicEnum, String tenant, String namespace, String message) {
        String topic = String.format(messageTopicEnum.getPulsar().getTopic(), tenant, namespace);
        MessageId messageId = stringPulsarTemplate.newMessage(message).withTopic(topic).send();
        return messageId.toString();
    }
}
