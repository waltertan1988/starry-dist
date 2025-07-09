package com.walter.starry.autoconfigure.mdc.message;

import com.walter.starry.common.function.MessageListenerPostProcessor;
import com.walter.starry.common.util.MdcUtil;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author walter.tan
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = {"rocketmq.name-server"})
public class RocketMqMessageListenerPostProcessor implements MessageListenerPostProcessor {

    @Override
    public void preHandle(Object... object) {
        MessageExt messageExt = (MessageExt) object[0];
        MdcUtil.setTraceId(messageExt.getUserProperty(MdcUtil.ATTR_TRACE_ID));
    }

    @Override
    public void postHandle(Object... objects) {
        MdcUtil.removeTraceId();
    }
}
