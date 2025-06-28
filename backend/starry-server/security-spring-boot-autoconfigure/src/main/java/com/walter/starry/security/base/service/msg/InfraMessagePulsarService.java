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
    public <T> String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, T msgObj){
        return this.publishToPulsar(messageTopicEnum, appMsgProps.getPulsar().getBaseReg().getTenant(), appMsgProps.getPulsar().getBaseReg().getNamespace(), msgObj);
    }

    public <T> String publishToPulsar(MessageTopicEnum messageTopicEnum, String tenant, String namespace, T msgObj) {
        String topic = String.format(messageTopicEnum.getPulsar().getTopic(), tenant, namespace);
        String message = JsonUtil.toJson(msgObj);

        try{
            MessageId messageId = stringPulsarTemplate
                    .newMessage(message)
                    .withTopic(topic)
                    .withMessageCustomizer(c -> c.property(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId()))
                    .send();
            return messageId.toString();
        }catch (Throwable t){
            throw new RuntimeException(String.format("publishToPulsar fail. topic: %s, message: %s", messageTopicEnum.name(), message), t);
        }
    }
}
