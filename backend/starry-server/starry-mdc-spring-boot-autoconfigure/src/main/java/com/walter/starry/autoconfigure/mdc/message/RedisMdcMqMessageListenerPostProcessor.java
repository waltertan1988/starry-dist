package com.walter.starry.autoconfigure.mdc.message;

import com.walter.starry.common.util.MdcUtil;
import com.walter.starry.common.vo.RedisMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author walter.tan
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.message.redis.enabled", havingValue = "true")
public class RedisMdcMqMessageListenerPostProcessor implements MdcMessageListenerPostProcessor {

    @Override
    public void preHandle(Object... object) {
        RedisMessage redisMessage = (RedisMessage) object[0];
        MdcUtil.setTraceId(redisMessage.getTraceId());
    }
}
