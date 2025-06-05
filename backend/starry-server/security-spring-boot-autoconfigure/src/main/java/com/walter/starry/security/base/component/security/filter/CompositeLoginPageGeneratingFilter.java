package com.walter.starry.security.base.component.security.filter;

import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.util.HttpServletRequestUtil;
import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.vo.response.base.LoginPageVo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 自定义组合生成多种登录页面数据的过滤器，用于替换默认的登录表单页。
 * 参考：{@link DefaultLoginPageGeneratingFilter}
 *
 * @Author: walter.tan
 * @DateTime: 2023-11-22 11:20:08
 */
@Slf4j
public class CompositeLoginPageGeneratingFilter extends GenericFilterBean {

    private String loginPageUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;

    private String logoutSuccessUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?logout";

    private String failureUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?" + DefaultLoginPageGeneratingFilter.ERROR_PARAMETER_NAME;

    private boolean formLoginEnabled;

    private boolean oauth2LoginEnabled;

    private boolean saml2LoginEnabled;

    private String authenticationUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;

    private String usernameParameter = UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY;

    private String passwordParameter = UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY;

    private String rememberMeParameter = AbstractRememberMeServices.DEFAULT_PARAMETER;

    private Map<String, String> oauth2AuthenticationUrlToClientName;

    private Map<String, String> saml2AuthenticationUrlToProviderName;

    private Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs = (request) -> Collections.emptyMap();

    // Ajax方式登录
    private final AjaxLoginPageGeneratingFilter ajaxLoginPageGeneratingFilter;
    // 非Ajax方式登录
    private final NonAjaxLoginPageGeneratingFilter nonAjaxLoginPageGeneratingFilter;

    public CompositeLoginPageGeneratingFilter(){
        ajaxLoginPageGeneratingFilter = this.ajaxLoginPageGeneratingFilter();
        nonAjaxLoginPageGeneratingFilter = this.nonAjaxLoginPageGeneratingFilter();
    }

    public CompositeLoginPageGeneratingFilter(String loginPageUrl, String logoutSuccessUrl, String failureUrl, String authenticationUrl,
                                              boolean formLoginEnabled, boolean oauth2LoginEnabled, Map<String, String> oauth2LoginUrlToClientName,
                                              boolean saml2LoginEnabled, String rememberMeParameter, Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs){
        this.loginPageUrl = loginPageUrl;
        this.logoutSuccessUrl = logoutSuccessUrl;
        this.failureUrl = failureUrl;
        this.authenticationUrl = authenticationUrl;
        this.formLoginEnabled = formLoginEnabled;
        this.oauth2LoginEnabled = oauth2LoginEnabled;
        this.oauth2AuthenticationUrlToClientName = oauth2LoginUrlToClientName;
        this.saml2LoginEnabled = saml2LoginEnabled;
        this.rememberMeParameter = rememberMeParameter;
        this.resolveHiddenInputs = resolveHiddenInputs;

        ajaxLoginPageGeneratingFilter = this.ajaxLoginPageGeneratingFilter();
        nonAjaxLoginPageGeneratingFilter = this.nonAjaxLoginPageGeneratingFilter();
    }

    /**
     * 登录表单和注销表单的隐藏域数据，参考{@link DefaultLoginPageConfigurer#init(HttpSecurityBuilder)}
     */
    public static Map<String, String> hiddenInputs(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return (token != null) ? Collections.singletonMap(token.getParameterName(), token.getToken())
                : Collections.emptyMap();
    }

    private AjaxLoginPageGeneratingFilter ajaxLoginPageGeneratingFilter(){
        AjaxLoginPageGeneratingFilter filter = new AjaxLoginPageGeneratingFilter();
        filter.setLoginPageUrl(this.loginPageUrl);
        filter.setLogoutSuccessUrl(this.logoutSuccessUrl);
        filter.setFailureUrl(this.failureUrl);
        filter.setAuthenticationUrl(this.authenticationUrl);
        filter.setFormLoginEnabled(this.formLoginEnabled);
        filter.setOauth2LoginEnabled(this.oauth2LoginEnabled);
        filter.setOauth2AuthenticationUrlToClientName(this.oauth2AuthenticationUrlToClientName);
        filter.setSaml2LoginEnabled(this.saml2LoginEnabled);
        filter.setRememberMeParameter(this.rememberMeParameter);
        filter.setResolveHiddenInputs(this.resolveHiddenInputs);
        return filter;
    }

    private NonAjaxLoginPageGeneratingFilter nonAjaxLoginPageGeneratingFilter(){
        NonAjaxLoginPageGeneratingFilter filter = new NonAjaxLoginPageGeneratingFilter();
        filter.setLoginPageUrl(this.loginPageUrl);
        filter.setLogoutSuccessUrl(this.logoutSuccessUrl);
        filter.setFailureUrl(this.failureUrl);
        filter.setAuthenticationUrl(this.authenticationUrl);
        filter.setFormLoginEnabled(this.formLoginEnabled);
        filter.setOauth2LoginEnabled(this.oauth2LoginEnabled);
        filter.setOauth2AuthenticationUrlToClientName(this.oauth2AuthenticationUrlToClientName);
        filter.setSaml2LoginEnabled(this.saml2LoginEnabled);
        filter.setRememberMeParameter(this.rememberMeParameter);
        filter.setResolveHiddenInputs(this.resolveHiddenInputs);
        return filter;
    }

    public void setResolveHiddenInputs(Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs) {
        Assert.notNull(resolveHiddenInputs, "resolveHiddenInputs cannot be null");
        this.resolveHiddenInputs = resolveHiddenInputs;
    }

    public boolean isEnabled() {
        return this.formLoginEnabled || this.oauth2LoginEnabled || this.saml2LoginEnabled;
    }

    public void setLogoutSuccessUrl(String logoutSuccessUrl) {
        this.logoutSuccessUrl = logoutSuccessUrl;
    }

    public String getLoginPageUrl() {
        return this.loginPageUrl;
    }

    public void setLoginPageUrl(String loginPageUrl) {
        this.loginPageUrl = loginPageUrl;
    }

    public void setFailureUrl(String failureUrl) {
        this.failureUrl = failureUrl;
    }

    public void setFormLoginEnabled(boolean formLoginEnabled) {
        this.formLoginEnabled = formLoginEnabled;
    }

    public void setOauth2LoginEnabled(boolean oauth2LoginEnabled) {
        this.oauth2LoginEnabled = oauth2LoginEnabled;
    }

    public void setSaml2LoginEnabled(boolean saml2LoginEnabled) {
        this.saml2LoginEnabled = saml2LoginEnabled;
    }

    public void setAuthenticationUrl(String authenticationUrl) {
        this.authenticationUrl = authenticationUrl;
    }

    public void setUsernameParameter(String usernameParameter) {
        this.usernameParameter = usernameParameter;
    }

    public void setPasswordParameter(String passwordParameter) {
        this.passwordParameter = passwordParameter;
    }

    public void setRememberMeParameter(String rememberMeParameter) {
        this.rememberMeParameter = rememberMeParameter;
    }

    public void setOauth2AuthenticationUrlToClientName(Map<String, String> oauth2AuthenticationUrlToClientName) {
        this.oauth2AuthenticationUrlToClientName = oauth2AuthenticationUrlToClientName;
    }

    public void setSaml2AuthenticationUrlToProviderName(Map<String, String> saml2AuthenticationUrlToProviderName) {
        this.saml2AuthenticationUrlToProviderName = saml2AuthenticationUrlToProviderName;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(HttpServletRequestUtil.isAjaxRequest((HttpServletRequest) request)){
            this.ajaxLoginPageGeneratingFilter.doFilter(request, response, chain);
        }else{
            this.nonAjaxLoginPageGeneratingFilter.doFilter(request, response, chain);
        }
    }

    /**
     * 自定义生成非Ajax登录页面数据的过滤器，用于替换默认的登录表单页。
     * 参考：{@link DefaultLoginPageGeneratingFilter}
     */
    @Slf4j
    private static class NonAjaxLoginPageGeneratingFilter extends GenericFilterBean {
        private String loginPageUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;
        private String logoutSuccessUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?logout";
        private String failureUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?" + DefaultLoginPageGeneratingFilter.ERROR_PARAMETER_NAME;
        private boolean formLoginEnabled;
        private boolean oauth2LoginEnabled;
        private boolean saml2LoginEnabled;
        private String authenticationUrl;
        private String usernameParameter = UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY;
        private String passwordParameter = UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY;
        private String rememberMeParameter;

        private Map<String, String> oauth2AuthenticationUrlToClientName;
        private Map<String, String> saml2AuthenticationUrlToProviderName;
        private Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs = (request) -> Collections.emptyMap();

        /**
         * Sets a Function used to resolve a Map of the hidden inputs where the key is the
         * name of the input and the value is the value of the input. Typically this is used
         * to resolve the CSRF token.
         * @param resolveHiddenInputs the function to resolve the inputs
         */
        public void setResolveHiddenInputs(Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs) {
            Assert.notNull(resolveHiddenInputs, "resolveHiddenInputs cannot be null");
            this.resolveHiddenInputs = resolveHiddenInputs;
        }

        public boolean isEnabled() {
            return this.formLoginEnabled || this.oauth2LoginEnabled || this.saml2LoginEnabled;
        }

        public void setLogoutSuccessUrl(String logoutSuccessUrl) {
            this.logoutSuccessUrl = logoutSuccessUrl;
        }

        public String getLoginPageUrl() {
            return this.loginPageUrl;
        }

        public void setLoginPageUrl(String loginPageUrl) {
            this.loginPageUrl = loginPageUrl;
        }

        public void setFailureUrl(String failureUrl) {
            this.failureUrl = failureUrl;
        }

        public void setFormLoginEnabled(boolean formLoginEnabled) {
            this.formLoginEnabled = formLoginEnabled;
        }

        public void setOauth2LoginEnabled(boolean oauth2LoginEnabled) {
            this.oauth2LoginEnabled = oauth2LoginEnabled;
        }

        public void setSaml2LoginEnabled(boolean saml2LoginEnabled) {
            this.saml2LoginEnabled = saml2LoginEnabled;
        }

        public void setAuthenticationUrl(String authenticationUrl) {
            this.authenticationUrl = authenticationUrl;
        }

        public void setUsernameParameter(String usernameParameter) {
            this.usernameParameter = usernameParameter;
        }

        public void setPasswordParameter(String passwordParameter) {
            this.passwordParameter = passwordParameter;
        }

        public void setRememberMeParameter(String rememberMeParameter) {
            this.rememberMeParameter = rememberMeParameter;
        }

        public void setOauth2AuthenticationUrlToClientName(Map<String, String> oauth2AuthenticationUrlToClientName) {
            this.oauth2AuthenticationUrlToClientName = oauth2AuthenticationUrlToClientName;
        }

        public void setSaml2AuthenticationUrlToProviderName(Map<String, String> saml2AuthenticationUrlToProviderName) {
            this.saml2AuthenticationUrlToProviderName = saml2AuthenticationUrlToProviderName;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        }

        private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            boolean loginError = isErrorPage(request);
            boolean logoutSuccess = isLogoutSuccess(request);
            if (isLoginUrlRequest(request) || loginError || logoutSuccess) {
                String loginPageHtml = generateLoginPageHtml(request, loginError, logoutSuccess);
                response.setContentType("text/html;charset=UTF-8");
                response.setContentLength(loginPageHtml.getBytes(StandardCharsets.UTF_8).length);
                response.getWriter().write(loginPageHtml);
                return;
            }
            chain.doFilter(request, response);
        }

        private String generateLoginPageHtml(HttpServletRequest request, boolean loginError, boolean logoutSuccess) {
            String errorMsg = loginError ? getLoginErrorMessage(request) : "Invalid credentials";
            String contextPath = request.getContextPath();
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>\n");
            sb.append("<html lang=\"en\">\n");
            sb.append("  <head>\n");
            sb.append("    <meta charset=\"utf-8\">\n");
            sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n");
            sb.append("    <meta name=\"description\" content=\"\">\n");
            sb.append("    <meta name=\"author\" content=\"\">\n");
            sb.append("    <title>Please sign in</title>\n");
            sb.append("    <link href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css\" "
                    + "rel=\"stylesheet\" integrity=\"sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M\" crossorigin=\"anonymous\">\n");
            sb.append("    <link href=\"https://getbootstrap.com/docs/4.0/examples/signin/signin.css\" "
                    + "rel=\"stylesheet\" crossorigin=\"anonymous\"/>\n");
            sb.append("  </head>\n");
            sb.append("  <body>\n");
            sb.append("     <div class=\"container\">\n");
            if (this.formLoginEnabled) {
                sb.append("      <form class=\"form-signin\" method=\"post\" action=\"" + contextPath
                        + this.authenticationUrl + "\">\n");
                sb.append("        <h2 class=\"form-signin-heading\">请先登录</h2>\n");
                sb.append(createError(loginError, errorMsg) + createLogoutSuccess(logoutSuccess) + "        <p>\n");
                sb.append("          <label for=\"username\" class=\"sr-only\">账号</label>\n");
                sb.append("          <input type=\"text\" id=\"username\" name=\"" + this.usernameParameter
                        + "\" class=\"form-control\" placeholder=\"账号\" required autofocus>\n");
                sb.append("        </p>\n");
                sb.append("        <p>\n");
                sb.append("          <label for=\"password\" class=\"sr-only\">密码</label>\n");
                sb.append("          <input type=\"password\" id=\"password\" name=\"" + this.passwordParameter
                        + "\" class=\"form-control\" placeholder=\"密码\" required>\n");
                sb.append("        </p>\n");
                sb.append(createRememberMe(this.rememberMeParameter) + renderHiddenInputs(request));
                sb.append("        <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">登录</button>\n");
                sb.append("      </form>\n");
            }
            if (this.oauth2LoginEnabled) {
                sb.append("<h2 class=\"form-signin-heading\">Login with OAuth 2.0</h2>");
                sb.append(createError(loginError, errorMsg));
                sb.append(createLogoutSuccess(logoutSuccess));
                sb.append("<table class=\"table table-striped\">\n");
                for (Map.Entry<String, String> clientAuthenticationUrlToClientName : this.oauth2AuthenticationUrlToClientName
                        .entrySet()) {
                    sb.append(" <tr><td>");
                    String url = clientAuthenticationUrlToClientName.getKey();
                    sb.append("<a href=\"").append(contextPath).append(url).append("\">");
                    String clientName = HtmlUtils.htmlEscape(clientAuthenticationUrlToClientName.getValue());
                    sb.append(clientName);
                    sb.append("</a>");
                    sb.append("</td></tr>\n");
                }
                sb.append("</table>\n");
            }
            if (this.saml2LoginEnabled) {
                sb.append("<h2 class=\"form-signin-heading\">Login with SAML 2.0</h2>");
                sb.append(createError(loginError, errorMsg));
                sb.append(createLogoutSuccess(logoutSuccess));
                sb.append("<table class=\"table table-striped\">\n");
                for (Map.Entry<String, String> relyingPartyUrlToName : this.saml2AuthenticationUrlToProviderName
                        .entrySet()) {
                    sb.append(" <tr><td>");
                    String url = relyingPartyUrlToName.getKey();
                    sb.append("<a href=\"").append(contextPath).append(url).append("\">");
                    String partyName = HtmlUtils.htmlEscape(relyingPartyUrlToName.getValue());
                    sb.append(partyName);
                    sb.append("</a>");
                    sb.append("</td></tr>\n");
                }
                sb.append("</table>\n");
            }
            sb.append("</div>\n");
            sb.append("</body></html>");
            return sb.toString();
        }

        private String getLoginErrorMessage(HttpServletRequest request) {
            HttpSession session = request.getSession(false);
            if (session != null &&
                    session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) instanceof AuthenticationException exception) {
                return exception.getMessage();
            }
            return "Invalid credentials";
        }

        private String renderHiddenInputs(HttpServletRequest request) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> input : this.resolveHiddenInputs.apply(request).entrySet()) {
                sb.append("<input name=\"");
                sb.append(input.getKey());
                sb.append("\" type=\"hidden\" value=\"");
                sb.append(input.getValue());
                sb.append("\" />\n");
            }
            return sb.toString();
        }

        private String createRememberMe(String paramName) {
            if (paramName == null) {
                return "";
            }
            return "<p><input type='checkbox' name='" + paramName + "'/> Remember me on this computer.</p>\n";
        }

        private boolean isLogoutSuccess(HttpServletRequest request) {
            return this.logoutSuccessUrl != null && matches(request, this.logoutSuccessUrl);
        }

        private boolean isLoginUrlRequest(HttpServletRequest request) {
            return matches(request, this.getLoginPageUrl());
        }

        private boolean isErrorPage(HttpServletRequest request) {
            return matches(request, this.failureUrl);
        }

        private String createError(boolean isError, String message) {
            if (!isError) {
                return "";
            }
            return "<div class=\"alert alert-danger\" role=\"alert\">" + HtmlUtils.htmlEscape(message) + "</div>";
        }

        private String createLogoutSuccess(boolean isLogoutSuccess) {
            if (!isLogoutSuccess) {
                return "";
            }
            return "<div class=\"alert alert-success\" role=\"alert\">您已成功退出系统</div>";
        }

        private boolean matches(HttpServletRequest request, String url) {
            if (!"GET".equals(request.getMethod()) || url == null) {
                return false;
            }
            String uri = request.getRequestURI();
            int pathParamIndex = uri.indexOf(';');
            if (pathParamIndex > 0) {
                // strip everything after the first semi-colon
                uri = uri.substring(0, pathParamIndex);
            }
            if (request.getQueryString() != null) {
                uri += "?" + request.getQueryString();
            }
            if ("".equals(request.getContextPath())) {
                return uri.equals(url);
            }
            return uri.equals(request.getContextPath() + url);
        }
    }

    /**
     * 自定义生成Ajax登录页面数据的过滤器，用于替换默认的登录表单页。
     * 参考：{@link DefaultLoginPageGeneratingFilter}
     */
    @Slf4j
    private static class AjaxLoginPageGeneratingFilter extends GenericFilterBean {

        private String loginPageUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;

        private String logoutSuccessUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?logout";

        private String failureUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?" + DefaultLoginPageGeneratingFilter.ERROR_PARAMETER_NAME;

        private boolean formLoginEnabled;

        private boolean oauth2LoginEnabled;

        private boolean saml2LoginEnabled;

        private String authenticationUrl = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;

        private String usernameParameter = UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY;

        private String passwordParameter = UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_PASSWORD_KEY;

        private String rememberMeParameter = AbstractRememberMeServices.DEFAULT_PARAMETER;

        private Map<String, String> oauth2AuthenticationUrlToClientName;

        private Map<String, String> saml2AuthenticationUrlToProviderName;

        private Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs = (request) -> Collections.emptyMap();

        public AjaxLoginPageGeneratingFilter() {
        }

        /**
         * 登录表单和注销表单的隐藏域数据，参考{@link DefaultLoginPageConfigurer#init(HttpSecurityBuilder)}
         */
        public static Map<String, String> hiddenInputs(HttpServletRequest request) {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            return (token != null) ? Collections.singletonMap(token.getParameterName(), token.getToken())
                    : Collections.emptyMap();
        }

        /**
         * Sets a Function used to resolve a Map of the hidden inputs where the key is the
         * name of the input and the value is the value of the input. Typically this is used
         * to resolve the CSRF token.
         * @param resolveHiddenInputs the function to resolve the inputs
         */
        public void setResolveHiddenInputs(Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs) {
            Assert.notNull(resolveHiddenInputs, "resolveHiddenInputs cannot be null");
            this.resolveHiddenInputs = resolveHiddenInputs;
        }

        public boolean isEnabled() {
            return this.formLoginEnabled || this.oauth2LoginEnabled || this.saml2LoginEnabled;
        }

        public void setLogoutSuccessUrl(String logoutSuccessUrl) {
            this.logoutSuccessUrl = logoutSuccessUrl;
        }

        public String getLoginPageUrl() {
            return this.loginPageUrl;
        }

        public void setLoginPageUrl(String loginPageUrl) {
            this.loginPageUrl = loginPageUrl;
        }

        public void setFailureUrl(String failureUrl) {
            this.failureUrl = failureUrl;
        }

        public void setFormLoginEnabled(boolean formLoginEnabled) {
            this.formLoginEnabled = formLoginEnabled;
        }

        public void setOauth2LoginEnabled(boolean oauth2LoginEnabled) {
            this.oauth2LoginEnabled = oauth2LoginEnabled;
        }

        public void setSaml2LoginEnabled(boolean saml2LoginEnabled) {
            this.saml2LoginEnabled = saml2LoginEnabled;
        }

        public void setAuthenticationUrl(String authenticationUrl) {
            this.authenticationUrl = authenticationUrl;
        }

        public void setUsernameParameter(String usernameParameter) {
            this.usernameParameter = usernameParameter;
        }

        public void setPasswordParameter(String passwordParameter) {
            this.passwordParameter = passwordParameter;
        }

        public void setRememberMeParameter(String rememberMeParameter) {
            this.rememberMeParameter = rememberMeParameter;
        }

        public void setOauth2AuthenticationUrlToClientName(Map<String, String> oauth2AuthenticationUrlToClientName) {
            this.oauth2AuthenticationUrlToClientName = oauth2AuthenticationUrlToClientName;
        }

        public void setSaml2AuthenticationUrlToProviderName(Map<String, String> saml2AuthenticationUrlToProviderName) {
            this.saml2AuthenticationUrlToProviderName = saml2AuthenticationUrlToProviderName;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        }

        private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            boolean loginError = isErrorPage(request);
            boolean logoutSuccess = isLogoutSuccess(request);
            if (isLoginUrlRequest(request) || loginError || logoutSuccess) {
                LoginPageVo loginPageVo = new LoginPageVo();
                loginPageVo.setLoginError(loginError);
                String errorMsg = loginError ? getLoginErrorMessage(request) : "Invalid credentials";
                loginPageVo.setLoginErrorMessage(HtmlUtils.htmlEscape(errorMsg));
                loginPageVo.setIsLogoutSuccess(logoutSuccess);

                String contextPath = request.getContextPath();
                if(this.formLoginEnabled){
                    LoginPageVo.FormLoginPageVo formLoginPageVo = new LoginPageVo.FormLoginPageVo();
                    formLoginPageVo.setUsernameParameter(this.usernameParameter);
                    formLoginPageVo.setPasswordParameter(this.passwordParameter);
                    formLoginPageVo.setHiddenInputs(this.resolveHiddenInputs.apply(request));
                    formLoginPageVo.setAuthenticationUrl(contextPath + this.authenticationUrl);
                    loginPageVo.setFormLoginPageVo(formLoginPageVo);
                }
                if (this.oauth2LoginEnabled) {
                    LoginPageVo.OAuth2LoginPageVo oAuth2LoginPageVo = new LoginPageVo.OAuth2LoginPageVo();
                    Map<String, String> clientAuthenticationUrlToClientNameMap = new HashMap<>();
                    for (var clientAuthenticationUrlToClientName : this.oauth2AuthenticationUrlToClientName.entrySet()) {
                        String url = contextPath + clientAuthenticationUrlToClientName.getKey();
                        String clientName = HtmlUtils.htmlEscape(clientAuthenticationUrlToClientName.getValue());
                        clientAuthenticationUrlToClientNameMap.put(url, clientName);
                    }
                    oAuth2LoginPageVo.setClientAuthenticationUrlToClientNameMap(clientAuthenticationUrlToClientNameMap);
                    loginPageVo.setOAuth2LoginPageVo(oAuth2LoginPageVo);
                }
                if (this.saml2LoginEnabled) {
                    LoginPageVo.Saml2LoginPageVo saml2LoginPageVo = new LoginPageVo.Saml2LoginPageVo();
                    Map<String, String> saml2AuthenticationUrlToProviderNameMap = new HashMap<>();
                    for (var relyingPartyUrlToName : this.saml2AuthenticationUrlToProviderName.entrySet()) {
                        String url = contextPath + relyingPartyUrlToName.getKey();
                        String partyName = HtmlUtils.htmlEscape(relyingPartyUrlToName.getValue());
                        saml2AuthenticationUrlToProviderNameMap.put(url, partyName);
                    }
                    saml2LoginPageVo.setRelyingPartyUrlToNameMap(saml2AuthenticationUrlToProviderNameMap);
                    loginPageVo.setSaml2LoginPageVo(saml2LoginPageVo);
                }

                String voJson = JsonUtil.toJson(ApiResponse.success(loginPageVo));
                response.setContentType("application/json;charset=UTF-8");
                if(StringUtils.hasText(voJson)){
                    response.setContentLength(voJson.getBytes(StandardCharsets.UTF_8).length);
                    response.getWriter().write(voJson);
                }
                return;
            }
            chain.doFilter(request, response);
        }

        private String getLoginErrorMessage(HttpServletRequest request) {
            HttpSession session = request.getSession(false);
            if (session != null &&
                    session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) instanceof AuthenticationException exception) {
                return exception.getMessage();
            }
            return "Invalid credentials";
        }

        private boolean isLogoutSuccess(HttpServletRequest request) {
            return this.logoutSuccessUrl != null && matches(request, this.logoutSuccessUrl);
        }

        private boolean isLoginUrlRequest(HttpServletRequest request) {
            return matches(request, this.loginPageUrl);
        }

        private boolean isErrorPage(HttpServletRequest request) {
            return matches(request, this.failureUrl);
        }

        private boolean matches(HttpServletRequest request, String url) {
            if (!"GET".equals(request.getMethod()) || url == null) {
                return false;
            }
            String uri = request.getRequestURI();
            int pathParamIndex = uri.indexOf(';');
            if (pathParamIndex > 0) {
                // strip everything after the first semi-colon
                uri = uri.substring(0, pathParamIndex);
            }
            if (request.getQueryString() != null) {
                uri += "?" + request.getQueryString();
            }
            if ("".equals(request.getContextPath())) {
                return uri.equals(url);
            }
            return uri.equals(request.getContextPath() + url);
        }
    }
}
