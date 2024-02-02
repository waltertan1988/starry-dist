package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * 清理用户已失效的会话集
 * @Author: walter.tan
 * @DateTime: 2024-02-02 22:07:42
 */
@Data
@Configuration
@ConfigurationProperties("app.scheduling.clear-user-expired-sessions")
public class AppSchedulingClearUserExpiredSessionsProperties {

    /**
     * cron表达式，默认-（禁用），参考：{@link Scheduled#cron}
     */
    private String cron = "-";

    /**
     * 执行时长，默认1min
     */
    private Duration runDuration = Duration.ofMinutes(1);
}
