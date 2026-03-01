package com.walter.starry.security.base.component.security.oauth2;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * OIDC查找关联到本应用的用户信息的服务接口
 * @Author: walter.tan
 * @DateTime: 2024-01-21 00:29:20
 */
public interface OidcUserDetailsService {

    /**
     * 根据OIDC的注册ID和OpenId查找本应用的用户信息
     * @param registrationId OAuth2授权服务器在客户端的注册ID
     * @param openId 用户在OAuth2授权服务器中的开放账号
     * @return
     * @throws UsernameNotFoundException
     */
    UserDetails loadUserByRegistrationIdAndOpenId(String registrationId, String openId) throws UsernameNotFoundException;
}
