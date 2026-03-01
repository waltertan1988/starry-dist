package com.walter.starry.common.core.message;

import org.springframework.core.Ordered;

/**
 * 消息队列监听器的后处理器
 * @author walter.tan
 */
public interface MessageListenerPostProcessor extends Ordered {

    int DEFAULT_ORDER = 0;

    /**
     * 消费消息的前置处理。如果该方法在执行过程中出现异常，请在本方法内自行处理反向逻辑
     * @param objects
     */
    void preHandle(Object... objects);

    /**
     * 消费消息的后置处理。如果前置处理方法{@link MessageListenerPostProcessor#preHandle(Object...)}抛出异常，此方法将不会执行
     * @param objects
     */
    void postHandle(Object... objects);

    /**
     * 定义该处理器在调用链中的位置
     * @return 负数一般用定义系统内置的处理器，具体的业务处理器建议使用非负数
     */
    @Override
    default int getOrder() {
        return DEFAULT_ORDER;
    }
}
