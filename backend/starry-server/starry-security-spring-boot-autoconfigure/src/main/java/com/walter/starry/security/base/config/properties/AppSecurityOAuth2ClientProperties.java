package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 应用的OAuth2客户端配置定义，参考：{@link OAuth2ClientProperties}
 * @Author: walter.tan
 * @DateTime: 2024-01-15 13:21:46
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security.oauth2.client")
public class AppSecurityOAuth2ClientProperties implements InitializingBean {

    /**
     * 是否启用OAuth2客户端
     */
    private Boolean enable = false;

    /**
     * OAuth provider details.
     */
    private final Map<String, OAuth2ClientProperties.Provider> provider = new HashMap<>();

    /**
     * OAuth client registrations.
     */
    private final Map<String, OAuth2ClientProperties.Registration> registration = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    public void validate() {
        getRegistration().values().forEach(this::validateRegistration);
    }

    private void validateRegistration(OAuth2ClientProperties.Registration registration) {
        if (!StringUtils.hasText(registration.getClientId())) {
            throw new IllegalStateException("Client id must not be empty.");
        }
    }
}
