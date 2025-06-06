package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author: walter.tan
 * @datetime: 2023/9/13 15:10
 */
@Data
@Configuration
@ConfigurationProperties("app.security")
public class AppSecurityProperties {
    /**
     * 命名空间前缀，包括session信息在Redis的前缀、Security相关缓存的前缀
     */
    private String namespacePrefix = "app";
    /**
     * 是否启用前后端分离配置，默认true
     * (注：false仅用于调试)
     */
    private boolean enableFrontBackSeperated = true;
    /**
     * 以非Ajax方式登录成功后，在Session没有RequestCache的情况下，默认重定向的目标url
     */
    private String defaultLoginSuccessTargetUrl = "/error";
}
