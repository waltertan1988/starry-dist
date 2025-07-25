package com.walter.starry.security.base.config;

import com.walter.starry.common.core.concurrent.ExtendedVirtualThreadExecutorPostProcessor;
import com.walter.starry.common.core.concurrent.ExtendedVirtualThreadExecutorPostProcessorChain;
import com.walter.starry.common.core.concurrent.ExtendedVirtualThreadExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-16 09:52:23
 */
@Slf4j
@Configuration
public class VirtualThreadExecutorServiceConfig {

    /**
     * 默认的虚拟线程后处理器链
     * @param processorList
     * @return
     */
    @Bean
    public ExtendedVirtualThreadExecutorPostProcessorChain defaultExtendedVirtualThreadExecutorPostProcessorChain(List<ExtendedVirtualThreadExecutorPostProcessor<Object>> processorList){
        return new ExtendedVirtualThreadExecutorPostProcessorChain(Objects.isNull(processorList) ? Collections.emptyList() : processorList);
    }

    /**
     * 后台系统默认的公共虚拟线程服务
     * @return
     */
    @Bean
    public ExtendedVirtualThreadExecutorService adminCommonVirtualThreadTaskExecutor(@Qualifier("defaultExtendedVirtualThreadExecutorPostProcessorChain") ExtendedVirtualThreadExecutorPostProcessorChain chain){
        return ExtendedVirtualThreadExecutorService.of(200, "admin-common-virtual-thread-", chain);
    }

    /**
     * Redis在PUB/SUB模式下，用于订阅消息的虚拟线程服务
     * @return
     */
    @Bean
    public ExtendedVirtualThreadExecutorService redisSubscribeVirtualThreadTaskExecutor(@Qualifier("defaultExtendedVirtualThreadExecutorPostProcessorChain") ExtendedVirtualThreadExecutorPostProcessorChain chain){
        return ExtendedVirtualThreadExecutorService.of(200, "redis-message-listener-virtual-thread-", chain);
    }

    /**
     * 默认的无界虚拟线程服务
     * @return
     */
    @Bean
    public ExtendedVirtualThreadExecutorService unboundedVirtualThreadTaskExecutor(@Qualifier("defaultExtendedVirtualThreadExecutorPostProcessorChain") ExtendedVirtualThreadExecutorPostProcessorChain chain){
        return ExtendedVirtualThreadExecutorService.of(Integer.MAX_VALUE, "unbounded-virtual-thread-",  chain);
    }
}
