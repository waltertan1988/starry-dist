package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用的Pulsar配置
 * @Author: walter.tan
 * @DateTime: 2024-03-27 22:33:50
 */
@Data
@Configuration
@ConfigurationProperties("app.pulsar")
public class AppPulsarProperties {
    /**
     * pulsar租户
     */
    private String tenant = "starry";
}
