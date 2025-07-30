package com.walter.starry.autoconfigure.mdc.message;

import com.walter.starry.common.core.message.MessageListenerPostProcessor;
import com.walter.starry.common.util.MdcUtil;
import org.springframework.core.Ordered;

/**
 * @author walter.tan
 */
public interface MdcMessageListenerPostProcessor extends MessageListenerPostProcessor {

    /**
     * 清理MDC
     * @param objects
     */
    @Override
    default void postHandle(Object... objects) {
        MdcUtil.removeTraceId();
    }

    /**
     * MDC处理器的优先级最高
     * @return 最高优先级
     */
    @Override
    default int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
