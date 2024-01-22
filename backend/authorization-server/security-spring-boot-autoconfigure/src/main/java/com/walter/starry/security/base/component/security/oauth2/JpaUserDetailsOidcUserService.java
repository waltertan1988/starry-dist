package com.walter.starry.security.base.component.security.oauth2;

import com.walter.starry.security.base.bo.MergedOidcUser;
import com.walter.starry.security.base.util.JsonUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Oidc用户信息与本地用户信息进行整合
 * @Author: walter.tan
 * @DateTime: 2024-01-20 21:50:18
 */
public class JpaUserDetailsOidcUserService extends OidcUserService {

    private final OidcUserDetailsService oidcUserDetailsService;

    public JpaUserDetailsOidcUserService(OidcUserDetailsService oidcUserDetailsService){
        this.oidcUserDetailsService = oidcUserDetailsService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        UserDetails userDetails = oidcUserDetailsService.loadUserByRegistrationIdAndOpenId(userRequest.getClientRegistration().getRegistrationId(), oidcUser.getName());

        if(Objects.isNull(userDetails)){
            return null;
        }

        // 检查本地用户权限编码是否与OIDC权限编码冲突
        Collection<String> oidcAuthoritySet = oidcUser.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        Collection<String> userDetailsAuthoritySet = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        Collection<String> conflictedAuthorities = CollectionUtils.intersection(oidcAuthoritySet, userDetailsAuthoritySet);
        if(CollectionUtils.isNotEmpty(conflictedAuthorities)){
            throw new OAuth2AuthenticationException("Conflicted authorities between local and oidc are detected: " + JsonUtil.toJson(conflictedAuthorities));
        }

        // 在退出登录时报Jackson序列化错误，参看配置：com.walter.starry.security.base.config.SessionConfig.objectMapper
        return new MergedOidcUser(oidcUser.getAuthorities(), userRequest.getIdToken(), oidcUser.getUserInfo(), userDetails);
    }
}
