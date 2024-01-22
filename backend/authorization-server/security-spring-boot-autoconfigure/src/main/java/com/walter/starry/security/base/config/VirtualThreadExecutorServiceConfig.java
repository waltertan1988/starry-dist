package com.walter.starry.security.base.config;

import com.walter.starry.security.base.common.concurrent.ExtendedVirtualThreadExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-16 09:52:23
 */
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
}
