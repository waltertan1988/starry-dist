package com.walter.starry.security.base.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 后台守护定时任务配置
 * @Author: walter.tan
 * @DateTime: 2024-02-02 21:34:56
 */
@EnableAsync
@EnableScheduling
@Configuration
public class SchedulingConfig {
}
