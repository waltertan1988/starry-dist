package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.component.pulsar.PulsarTopicConfig;
import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.util.MdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.MessageId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Service;

/**
 * 基础应用的Pulsar消息发送服务
 * @Author: walter.tan
 * @DateTime: 2025-06-17 14:03:28
 */
@Slf4j
@Service
@ConditionalOnBean(PulsarTopicConfig.class)
public class InfraMessagePulsarService extends AbstractInfraMessageService {
    @Autowired
    private PulsarTemplate<String> stringPulsarTemplate;

    @Override
    public <T> String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, T msgObj) throws Exception {
        String message = JsonUtil.toJson(msgObj);
        try{
            return this.sendBroadcastMessage(messageTopicEnum, message);
        }catch (Throwable t){
            throw new Exception(String.format("sendBroadcastMessage fail. topic: %s, message: %s", messageTopicEnum.name(), message), t);
        }
    }

    @Override
    public String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, String message) {
        return this.publishToPulsar(messageTopicEnum, appMsgProps.getPulsar().getBaseReg().getTenant(), appMsgProps.getPulsar().getBaseReg().getNamespace(), message);
    }

    public String publishToPulsar(MessageTopicEnum messageTopicEnum, String tenant, String namespace, String message) {
        String topic = String.format(messageTopicEnum.getPulsar().getTopic(), tenant, namespace);
        MessageId messageId = stringPulsarTemplate.newMessage(message)
                .withMessageCustomizer(c -> c.property(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId()))
                .withTopic(topic)
                .send();
        return messageId.toString();
    }
}
