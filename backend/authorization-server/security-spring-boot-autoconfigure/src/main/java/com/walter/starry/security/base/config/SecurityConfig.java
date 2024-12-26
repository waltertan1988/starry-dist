package com.walter.starry.security.base.config;

import com.walter.starry.security.base.component.security.*;
import com.walter.starry.security.base.component.security.filter.CompositeLoginPageGeneratingFilter;
import com.walter.starry.security.base.component.security.filter.CompositeLogoutPageGeneratingFilter;
import com.walter.starry.security.base.config.properties.AppSecurityProperties;
import com.walter.starry.security.base.mapper.AclUserMapper;
import com.walter.starry.security.base.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import javax.sql.DataSource;
import java.util.*;

/**
 * Spring Security配置
 * @author: walter.tan
 * @datetime: 2023/8/27 15:47
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private OAuth2SecurityConfig oAuth2SecurityConfig;
    @Autowired
    private AppSecurityProperties appSecurityProperties;
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;


    /** 登录相关操作的URL */
    public static final String LOGIN_OP_URL = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;
    /** 注销相关操作的URL */
    private static final String LOGOUT_OP_URL = CompositeLogoutPageGeneratingFilter.DEFAULT_LOGOUT_OP_URL;

    /**
     * Spring Security的基础过滤链
     * @param http
     * @param authz
     * @return
     * @throws Exception
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, @Qualifier("openPolicyAgentAuthorizationManager") AuthorizationManager<RequestAuthorizationContext> authz) throws Exception {
        if(appSecurityProperties.isEnableFrontBackSeperated()){
            // 用自定义的登录/注销页面数据生成Filter替换默认的html页面生成Filter
            this.replaceLoginLogoutPageGeneratingFilters(http);
        }

        http
            .authorizeHttpRequests(authorize -> authorize
                // 使用独立式授权管理器（数据库配置方式）
                // 参考：https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#remote-authorization-manager
                .anyRequest().access(authz)

                // 静态配置方式
//                .requestMatchers(SecurityConfig.permitRequestUrlPatterns()).permitAll()
//                .requestMatchers(HttpMethod.GET, "/admin/**").hasAnyRole("USER")
//                .anyRequest().authenticated()
            )
            .exceptionHandling(conf -> {
                if(appSecurityProperties.isEnableFrontBackSeperated()){
                    // 自定义匿名（含session过期）拒绝访问的处理器
                    conf.authenticationEntryPoint(new AjaxAuthenticationEntryPoint());
                    // 自定义权限拒绝访问的处理器
                    conf.accessDeniedHandler(new AjaxAccessDeniedHandler());
                }
            })
            // Form login handles the redirect to the login page from the
            // authorization server filter chain
            .formLogin(form -> {
                if(appSecurityProperties.isEnableFrontBackSeperated()){
                    // 支持Ajax与非Ajax登录失败的处理器
                    form.failureHandler(this.getAjaxSupportedAuthenticationFailureHandler(http));
                    // 支持Ajax与非Ajax登录成功的处理器
                    form.successHandler(this.getAjaxSupportedSavedRequestAwareAuthenticationSuccessHandler(http));
                }
            })
            .csrf(customizer -> {
                // 自定义检查受CSRF保护URL的RequestMatcher
                customizer.requireCsrfProtectionMatcher(new RequireCsrfProtectionRequestMatcher(requireCsrfProtectionUrls()));
            })
            .logout(logout -> {
                // LogoutFilter定义的默认处理注销请求的endpoint是/logout，这里保持一致即可
                logout.logoutUrl(LOGOUT_OP_URL);
                
                if(appSecurityProperties.isEnableFrontBackSeperated()){
                    // 支持Ajax与非Ajax退出登录成功的处理器
                    final String defaultLogoutSuccessUrl = LOGIN_OP_URL + "?logout";
                    logout.logoutSuccessHandler(new AjaxSupportedLogoutSuccessHandler(defaultLogoutSuccessUrl));
                }
            });

        if(oAuth2SecurityConfig.isAppSecurityOAuth2ClientEnabled()){
            // 支持OAuth2方式登录
            http.oauth2Login(oauth2 -> {
                if(appSecurityProperties.isEnableFrontBackSeperated()){
                    // 支持Ajax与非Ajax登录失败的处理器
                    oauth2.failureHandler(this.getAjaxSupportedAuthenticationFailureHandler(http));
                    // 支持Ajax与非Ajax登录成功的处理器
                    oauth2.successHandler(this.getAjaxSupportedSavedRequestAwareAuthenticationSuccessHandler(http));
                }

                // Oidc用户信息与本地用户信息进行整合
                // (参考：https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-oidc-user-service)
                oauth2.userInfoEndpoint(userInfo ->
                    userInfo.oidcUserService(applicationContext.getBean(OidcUserService.class))
                );
            });
        }

        return http.build();
    }

    /**
     * 独立式授权管理器（数据库配置方式）
     * @param aclAuthorityItemRepository
     * @param aclResourceItemRepository
     * @param aclAuthorityResourceRepository
     * @return
     */
    @Bean("openPolicyAgentAuthorizationManager")
    public OpenPolicyAgentAuthorizationManager openPolicyAgentAuthorizationManager(AclAuthorityItemRepository aclAuthorityItemRepository, AclResourceItemRepository aclResourceItemRepository, AclAuthorityResourceRepository aclAuthorityResourceRepository){
        return new OpenPolicyAgentAuthorizationManager(aclAuthorityItemRepository, aclResourceItemRepository, aclAuthorityResourceRepository);
    }

    /**
     * 用自定义的登录/注销页面数据生成Filter替换默认的html页面生成Filter
     * @param http
     */
    private void replaceLoginLogoutPageGeneratingFilters(HttpSecurity http) {
        // 用自定义生成登录页面数据的过滤器，替换默认的登录表单页（注释掉此行，将还原为默认的表单登录页）
        CompositeLoginPageGeneratingFilter compositeLoginPageGeneratingFilter = this.compositeLoginPageGeneratingFilter();
        http.addFilterBefore(compositeLoginPageGeneratingFilter, DefaultLoginPageGeneratingFilter.class);

        // 用自定义生成Ajax注销页面数据的过滤器，替换默认的注销表单页（注释掉此行，将还原为默认的表单注销页）
        http.addFilterBefore(this.compositeLogoutPageGeneratingFilter(), DefaultLogoutPageGeneratingFilter.class);

        // 参考org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer.init
        http.setSharedObject(CompositeLoginPageGeneratingFilter.class, compositeLoginPageGeneratingFilter);
    }

    /**
     * 无需认证的URL
     * @return
     */
    public static String[] permitRequestUrlPatterns(){
        return new String[]{LOGIN_OP_URL, "/favicon.ico", "/error"};
    }

    /**
     * 受CSRF机制保护的URL格式
     * @return
     */
    private static List<String> requireCsrfProtectionUrls(){
        List<String> list = new ArrayList<>();
        list.add(LOGIN_OP_URL);
        return list;
    }

    private CompositeLoginPageGeneratingFilter compositeLoginPageGeneratingFilter() {
        final String logoutSuccessUrl = LOGIN_OP_URL + "?logout";
        final String failureUrl = LOGIN_OP_URL + "?" + DefaultLoginPageGeneratingFilter.ERROR_PARAMETER_NAME;

        // 获取OAuth2客户端配置
        Map<String, String> oauth2LoginUrlToClientName = new HashMap<>();
        if (oAuth2SecurityConfig.isAppSecurityOAuth2ClientEnabled()) {
            if(clientRegistrationRepository instanceof InMemoryClientRegistrationRepository repo){
                // 参考：org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer.initDefaultLoginFilter
                repo.forEach((registration) -> {
                    if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType())) {
                        String authorizationRequestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + registration.getRegistrationId();
                        oauth2LoginUrlToClientName.put(authorizationRequestUri, registration.getClientName());
                    }
                });
            }else{
                throw new RuntimeException();
            }
        }

        // 不使用RememberMe功能
        return new CompositeLoginPageGeneratingFilter(LOGIN_OP_URL, logoutSuccessUrl, failureUrl, LOGIN_OP_URL,
                true, oAuth2SecurityConfig.isAppSecurityOAuth2ClientEnabled(), oauth2LoginUrlToClientName, false,
                null, CompositeLoginPageGeneratingFilter::hiddenInputs);
    }

    private CompositeLogoutPageGeneratingFilter compositeLogoutPageGeneratingFilter(){
        return new CompositeLogoutPageGeneratingFilter(LOGOUT_OP_URL, LOGOUT_OP_URL, CompositeLoginPageGeneratingFilter::hiddenInputs);
    }

    @Bean
    public JpaUserDetailsService userDetailsService(DataSource dataSource,
                                                    AclUserMapper aclUserMapper,
                                                    AclAuthorityRepository aclAuthorityRepository,
                                                    FindByIndexNameSessionRepository<? extends Session> findByIndexNameSessionRepository) {
        return new JpaUserDetailsService(dataSource, aclUserMapper, aclAuthorityRepository, findByIndexNameSessionRepository);
    }

    private AjaxSupportedAuthenticationFailureHandler getAjaxSupportedAuthenticationFailureHandler(HttpSecurity http){
        AjaxSupportedAuthenticationFailureHandler handler = http.getSharedObject(AjaxSupportedAuthenticationFailureHandler.class);
        if(Objects.nonNull(handler)){
            return handler;
        }

        final String defaultLoginFailureUrl = LOGIN_OP_URL + "?" + DefaultLoginPageGeneratingFilter.ERROR_PARAMETER_NAME;
        handler = new AjaxSupportedAuthenticationFailureHandler(defaultLoginFailureUrl);
        http.setSharedObject(AjaxSupportedAuthenticationFailureHandler.class, handler);
        return handler;
    }

    private AjaxSupportedSavedRequestAwareAuthenticationSuccessHandler getAjaxSupportedSavedRequestAwareAuthenticationSuccessHandler(HttpSecurity http){
        AjaxSupportedSavedRequestAwareAuthenticationSuccessHandler handler = http.getSharedObject(AjaxSupportedSavedRequestAwareAuthenticationSuccessHandler.class);
        if(Objects.nonNull(handler)){
            return handler;
        }

        handler = new AjaxSupportedSavedRequestAwareAuthenticationSuccessHandler(appSecurityProperties.getDefaultLoginSuccessTargetUrl());
        http.setSharedObject(AjaxSupportedSavedRequestAwareAuthenticationSuccessHandler.class, handler);
        return handler;
    }
}
