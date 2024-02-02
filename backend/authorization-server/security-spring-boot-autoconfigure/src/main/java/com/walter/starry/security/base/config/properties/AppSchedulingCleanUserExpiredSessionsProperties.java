package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * 清理用户已失效会话集的配置项
 * @Author: walter.tan
 * @DateTime: 2024-02-02 22:07:42
 */
@Data
@Configuration
@ConfigurationProperties("app.scheduling.clean-user-expired-sessions")
public class AppSchedulingCleanUserExpiredSessionsProperties {
    /**
     * cron表达式，默认-（禁用），参考：{@link Scheduled#cron}
     */
    private String cron = "-";
    /**
     * 最大执行时长，超过此时间的执行将不触发（可等待下一轮触发）。默认1分钟。
     */
    private Duration runDuration = Duration.ofMinutes(1);
    /**
     * 对同一用户执行清理的最小间隔天数，默认30天
     */
    private int cleanFromDaysBefore = 30;
}
