package com.walter.starry.autoconfigure.mdc.message;

import com.walter.starry.common.util.MdcUtil;
import org.apache.pulsar.client.api.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

/**
 * @author walter.tan
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = {"spring.pulsar.client.service-url", "spring.pulsar.admin.service-url"})
public class PulsarMqMdcMessageListenerPostProcessor implements MdcMessageListenerPostProcessor {

    @Override
    public void preHandle(Object... object) {
        Stream.of(object).filter(o -> o instanceof Message<?>).findFirst().ifPresent(message -> {
            String traceId = ((Message<?>)message).getProperty(MdcUtil.ATTR_TRACE_ID);
            if(StringUtils.hasText(traceId)){
                MdcUtil.setTraceId(traceId);
            }
        });
    }
}
