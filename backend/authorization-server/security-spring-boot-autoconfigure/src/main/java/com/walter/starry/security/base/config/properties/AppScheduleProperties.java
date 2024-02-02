package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: walter.tan
 * @DateTime: 2024-02-02 22:07:42
 */
@Data
@Configuration
@ConfigurationProperties("app.scheduling")
public class AppScheduleProperties {

    /**
     * 清理用户已失效的会话集
     */
    private String clearUserExpiredSessions = "-";
}
