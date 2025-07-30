package com.walter.starry.common.core.concurrent;

import org.springframework.core.Ordered;

/**
 * 扩展虚拟线程执行的后处理器
 * @author walter.tan
 */
public interface ExtendedVirtualThreadExecutorPostProcessor<T> extends Ordered {

    int DEFAULT_ORDER = 0;

    /**
     * 池内线程执行业务的前置处理。如果该方法在执行过程中出现异常，请在本方法内自行处理反向逻辑
     * @param context
     */
    void preHandle(T context);

    /**
     * 池内线程执行业务的后置处理。如果前置处理方法{@link ExtendedVirtualThreadExecutorPostProcessor#preHandle(Object)}抛出异常，此方法将不会执行
     * @param context
     */
    void postHandle(T context);

    /**
     * 从父线程获取本处理器所需的上下文对象
     * @return
     */
    T getContext();

    /**
     * 定义该处理器在调用链中的位置
     * @return 负数一般用定义系统内置的处理器，具体的业务处理器建议使用非负数
     */
    @Override
    default int getOrder() {
        return DEFAULT_ORDER;
    }
}
