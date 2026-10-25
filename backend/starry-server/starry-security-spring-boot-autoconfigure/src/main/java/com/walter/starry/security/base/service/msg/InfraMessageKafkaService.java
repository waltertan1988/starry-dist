package com.walter.starry.security.base.service.msg;

import com.walter.starry.common.constant.MqConstant;
import com.walter.starry.common.util.IdUtil;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.util.MdcUtil;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(name = MqConstant.Kafka.CONDITIONAL_ON_PROP)
public class InfraMessageKafkaService extends AbstractInfraMessageService{
    @Value("${spring.application.name}")
    private String appName;
    @Autowired
    @Qualifier("infraKafkaTemplate")
    private KafkaTemplate<String, String> infraKafkaTemplate;

    @Override
    public <T> String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, T msgObj) {
        String msgId = IdUtil.getUUID();
        String topic = MqConstant.Kafka.INFRA_BROADCAST_TOPIC.formatted(appName);
        List<String> message = List.of(messageTopicEnum.getKafka().getTopic(), JsonUtil.toJson(msgObj));

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, JsonUtil.toJson(message));
        record.headers().add(MqConstant.Kafka.HEADER_KEY_MSG_ID, msgId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(MdcUtil.ATTR_TRACE_ID, Optional.ofNullable(MdcUtil.getTraceId()).map(s -> s.getBytes(StandardCharsets.UTF_8)).orElse(null));
        infraKafkaTemplate.send(record);
        return msgId;
    }
}
