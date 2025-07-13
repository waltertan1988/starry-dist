package com.walter.starry.security.base.config;

import com.walter.starry.common.core.concurrent.ExtendedVirtualThreadExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-16 09:52:23
 */
@Slf4j
@Configuration
public class VirtualThreadExecutorServiceConfig {

    /**
     * 后台系统默认的公共虚拟线程服务
     * @return
     */
    @Bean
    public ExtendedVirtualThreadExecutorService adminCommonVirtualThreadTaskExecutor(){
        return ExtendedVirtualThreadExecutorService.of(200, "admin-common-virtual-thread-");
    }

    /**
     * Redis在PUB/SUB模式下，用于订阅消息的虚拟线程服务
     * @return
     */
    @Bean
    public ExtendedVirtualThreadExecutorService redisSubscribeVirtualThreadTaskExecutor(){
        return ExtendedVirtualThreadExecutorService.of(200, "redis-message-listener-virtual-thread-");
    }

    /**
     * 默认的无界虚拟线程服务
     * @return
     */
    @Bean
    public ExecutorService unboundedVirtualThreadTaskExecutor(){
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name("unbounded-virtual-thread-", 1)
                .uncaughtExceptionHandler((thread, throwable) -> log.error("Virtual thread {}", thread.getName(), throwable))
                .factory());
    }
}
