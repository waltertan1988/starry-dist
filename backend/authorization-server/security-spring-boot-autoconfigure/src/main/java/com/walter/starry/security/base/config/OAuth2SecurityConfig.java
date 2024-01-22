package com.walter.starry.security.base.config;

import com.walter.starry.security.base.component.security.oauth2.JpaUserDetailsOidcUserService;
import com.walter.starry.security.base.component.security.oauth2.OidcUserDetailsService;
import com.walter.starry.security.base.config.properties.AppSecurityOAuth2ClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security OAuth2配置
 * @Author: walter.tan
 * @DateTime: 2024-01-17 09:57:03
 */
@Slf4j
@Configuration
public class OAuth2SecurityConfig {
    private static final String APP_SECURITY_OAUTH2_CLIENT_ENABLE = "app.security.oauth2.client.enable";

    @Autowired
    private AppSecurityOAuth2ClientProperties appSecurityOAuth2ClientProperties;

    /**
     * 配置支持OAuth2客户端登录
     * @return
     */
    @Bean
    @ConditionalOnProperty(value = APP_SECURITY_OAUTH2_CLIENT_ENABLE, havingValue = "true")
    public ClientRegistrationRepository clientRegistrationRepository() {
        log.info("creating clientRegistrationRepository.");

        List<ClientRegistration> clientRegistrationList = appSecurityOAuth2ClientProperties.getRegistration().entrySet().stream().map(entry -> {
            String registrationId = entry.getKey();
            OAuth2ClientProperties.Registration registration = entry.getValue();

            OAuth2ClientProperties.Provider provider = appSecurityOAuth2ClientProperties.getProvider().get(registration.getProvider());

            return ClientRegistration.withRegistrationId(registrationId)
                    .clientId(registration.getClientId())
                    .clientSecret(registration.getClientSecret())
                    .clientAuthenticationMethod(new ClientAuthenticationMethod(registration.getClientAuthenticationMethod()))
                    .authorizationGrantType(new AuthorizationGrantType(registration.getAuthorizationGrantType()))
                    .redirectUri(registration.getRedirectUri())
                    .scope(registration.getScope())
                    .clientName(registration.getClientName())
                    .authorizationUri(provider.getAuthorizationUri())
                    .tokenUri(provider.getTokenUri())
                    .userInfoUri(provider.getUserInfoUri())
                    .userNameAttributeName(provider.getUserNameAttribute())
                    .jwkSetUri(provider.getJwkSetUri())
                    .build();
        }).collect(Collectors.toList());

        return new InMemoryClientRegistrationRepository(clientRegistrationList);
    }

    /**
     * 配置OAuth2客户端从授权服务器获取token后对token的存储方式
     * 参考：https://docs.spring.io/spring-security/reference/servlet/oauth2/client/core.html#oauth2Client-authorized-repo-service
     * @return
     */
    @Bean
    @ConditionalOnProperty(value = APP_SECURITY_OAUTH2_CLIENT_ENABLE, havingValue = "true")
    public OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository(){
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    @ConditionalOnProperty(value = APP_SECURITY_OAUTH2_CLIENT_ENABLE, havingValue = "true")
    public OidcUserService jpaUserDetailsOidcUserService(OidcUserDetailsService oidcUserDetailsService){
        return new JpaUserDetailsOidcUserService(oidcUserDetailsService);
    }

    /**
     * 是否启用OAuth2客户端
     * @return
     */
    public boolean isAppSecurityOAuth2ClientEnabled(){
        return BooleanUtils.toBoolean(appSecurityOAuth2ClientProperties.getEnable());
    }
}
