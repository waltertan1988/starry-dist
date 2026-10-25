package com.walter.starry.autoconfigure.mdc.message;

import com.walter.starry.common.constant.MqConstant;
import com.walter.starry.common.util.MdcUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author walter.tan
 */
@Component
@ConditionalOnProperty(name = MqConstant.Kafka.CONDITIONAL_ON_PROP)
public class KafkaMqMdcMessageListenerPostProcessor implements MdcMessageListenerPostProcessor {

    @Override
    public void preHandle(Object... object) {
        Stream.of(object).filter(o -> o instanceof ConsumerRecord<?, ?>).findFirst().ifPresent(message -> {
            String traceId = Optional.ofNullable(((ConsumerRecord<?,?>)message).headers().lastHeader(MdcUtil.ATTR_TRACE_ID)).map(Header::value).map(s -> new String(s, StandardCharsets.UTF_8)).orElse(null);
            if(StringUtils.hasText(traceId)){
                MdcUtil.setTraceId(traceId);
            }
        });
    }
}
