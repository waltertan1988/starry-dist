package com.walter.starry.common.function;

/**
 * 消息队列监听器的后处理器
 * @author walter.tan
 */
public interface MessageListenerPostProcessor {

    /**
     * 消费消息的前置处理
     * @param objects
     */
    void preHandle(Object... objects);

    /**
     * 消费消息的后置处理
     * @param objects
     */
    void postHandle(Object... objects);
}
