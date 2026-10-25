package com.walter.starry.security.base.component.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnClass({ProducerListener.class})
public final class KafkaSendResultListener<K, V> extends LoggingProducerListener<K, V> {

    @Override
    public void onSuccess(ProducerRecord<K, V> record, RecordMetadata recordMetadata) {
        log.info("kafka send success. topic:{}, partition:{}, key:{}, value:{}, recordMetadata:{}", record.topic(), record.partition(), record.key(), record.value(), recordMetadata);
        super.onSuccess(record, recordMetadata);
    }

    @Override
    public void onError(ProducerRecord<K, V> record, @Nullable RecordMetadata recordMetadata, Exception exception) {
        log.error("kafka send fail. topic:{}, partition:{}, key:{}, value:{}", record.topic(), record.partition(), record.key(), record.value(), exception);
        super.onError(record, recordMetadata, exception);
    }
}
