package com.walter.starry.autoconfigure.mdc.message;

import com.walter.starry.common.core.MessageListenerPostProcessor;
import com.walter.starry.common.util.MdcUtil;

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
}
