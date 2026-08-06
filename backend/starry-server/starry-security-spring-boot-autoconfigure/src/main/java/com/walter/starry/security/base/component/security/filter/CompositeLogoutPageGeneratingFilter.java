package com.walter.starry.security.base.component.security.filter;

import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.vo.ApiResponse;
import com.walter.starry.security.base.util.HttpServletRequestUtil;
import com.walter.starry.security.base.vo.response.base.LogoutPageVo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * 自定义组合生成多种注销页面数据的过滤器，用于替换默认的注销表单页。
 * 参考：{@link DefaultLogoutPageGeneratingFilter}
 * @Author: walter.tan
 * @DateTime: 2023-11-22 15:26:50
 */
public class CompositeLogoutPageGeneratingFilter extends OncePerRequestFilter {

    /** 默认的注销操作相关的url */
    public static final String DEFAULT_LOGOUT_OP_URL = "/logout";

    private Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs = (request) -> Collections.emptyMap();

    // Ajax注销
    private final AjaxLogoutPageGeneratingFilter ajaxLogoutPageGeneratingFilter;
    // 非Ajax注销
    private final NonAjaxLogoutPageGeneratingFilter nonAjaxLogoutPageGeneratingFilter;

    public CompositeLogoutPageGeneratingFilter(){
        this.ajaxLogoutPageGeneratingFilter = new AjaxLogoutPageGeneratingFilter();
        this.nonAjaxLogoutPageGeneratingFilter = new NonAjaxLogoutPageGeneratingFilter();
    }

    public CompositeLogoutPageGeneratingFilter(String logoutPageUrl, String logoutUrl, Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs){
        this.ajaxLogoutPageGeneratingFilter = new AjaxLogoutPageGeneratingFilter(logoutPageUrl, logoutUrl);
        this.ajaxLogoutPageGeneratingFilter.setResolveHiddenInputs(resolveHiddenInputs);

        this.nonAjaxLogoutPageGeneratingFilter = new NonAjaxLogoutPageGeneratingFilter();
        this.nonAjaxLogoutPageGeneratingFilter.setResolveHiddenInputs(resolveHiddenInputs);
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(HttpServletRequestUtil.isAjaxRequest(request)){
            ajaxLogoutPageGeneratingFilter.doFilterInternal(request, response, filterChain);
        }else{
            nonAjaxLogoutPageGeneratingFilter.doFilterInternal(request, response, filterChain);
        }
    }

    /**
     * 自定义生成非Ajax注销页面数据的过滤器，用于替换默认的注销表单页。
     *  参考：{@link DefaultLogoutPageGeneratingFilter}
     */
    private static class NonAjaxLogoutPageGeneratingFilter extends OncePerRequestFilter {
        /** 提交注销请求的url */
        private final String logoutUrl;
        private final RequestMatcher matcher;
        private Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs = (request) -> Collections.emptyMap();

        public NonAjaxLogoutPageGeneratingFilter(){
            this(DEFAULT_LOGOUT_OP_URL, DEFAULT_LOGOUT_OP_URL);
        }

        public NonAjaxLogoutPageGeneratingFilter(String logoutPageUrl, String logoutUrl){
            this.matcher = PathPatternRequestMatcher.pathPattern(HttpMethod.GET, logoutPageUrl);
            this.logoutUrl = logoutUrl;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            if (this.matcher.matches(request)) {
                renderLogout(request, response);
            }
            else {
                if (logger.isTraceEnabled()) {
                    logger.trace(LogMessage.format("Did not render default logout page since request did not match [%s]",
                            this.matcher));
                }
                filterChain.doFilter(request, response);
            }
        }

        private void renderLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>\n");
            sb.append("<html lang=\"en\">\n");
            sb.append("  <head>\n");
            sb.append("    <meta charset=\"utf-8\">\n");
            sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n");
            sb.append("    <meta name=\"description\" content=\"\">\n");
            sb.append("    <meta name=\"author\" content=\"\">\n");
            sb.append("    <title>退出系统</title>\n");
            sb.append("    <link href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css\" "
                    + "rel=\"stylesheet\" integrity=\"sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M\" "
                    + "crossorigin=\"anonymous\">\n");
            sb.append("    <link href=\"https://getbootstrap.com/docs/4.0/examples/signin/signin.css\" "
                    + "rel=\"stylesheet\" crossorigin=\"anonymous\"/>\n");
            sb.append("  </head>\n");
            sb.append("  <body>\n");
            sb.append("     <div class=\"container\">\n");
            sb.append("      <form class=\"form-signin\" method=\"post\" action=\"" + request.getContextPath()
                    + logoutUrl + "\">\n");
            sb.append("        <h2 class=\"form-signin-heading\">您确定要退出吗？</h2>\n");
            sb.append(renderHiddenInputs(request)
                    + "        <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">退出</button>\n");
            sb.append("      </form>\n");
            sb.append("    </div>\n");
            sb.append("  </body>\n");
            sb.append("</html>");
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(sb.toString());
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
    }

    /**
     * 自定义生成Ajax注销页面数据的过滤器，用于替换默认的注销表单页。
     *  参考：{@link DefaultLogoutPageGeneratingFilter}
     */
    private static class AjaxLogoutPageGeneratingFilter extends OncePerRequestFilter {
        /** 提交注销请求的url */
        private final String logoutUrl;
        private final RequestMatcher matcher;

        private Function<HttpServletRequest, Map<String, String>> resolveHiddenInputs = (request) -> Collections.emptyMap();

        public AjaxLogoutPageGeneratingFilter(){
            this(DEFAULT_LOGOUT_OP_URL, DEFAULT_LOGOUT_OP_URL);
        }

        public AjaxLogoutPageGeneratingFilter(String logoutPageUrl, String logoutUrl){
            this.matcher = PathPatternRequestMatcher.pathPattern(HttpMethod.GET, logoutPageUrl);
            this.logoutUrl = logoutUrl;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (this.matcher.matches(request)) {
                renderLogout(request, response);
            }
            else {
                if (logger.isTraceEnabled()) {
                    logger.trace(LogMessage.format("Did not render default logout page since request did not match [%s]",
                            this.matcher));
                }
                filterChain.doFilter(request, response);
            }
        }

        private void renderLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
            LogoutPageVo logoutPageVo = new LogoutPageVo();
            logoutPageVo.setLogoutUrl(this.logoutUrl);
            logoutPageVo.setHiddenInputs(this.resolveHiddenInputs.apply(request));

            String voJson = JsonUtil.toJson(ApiResponse.success(logoutPageVo));
            response.setContentType("application/json;charset=UTF-8");
            if(StringUtils.hasText(voJson)){
                response.setContentLength(voJson.getBytes(StandardCharsets.UTF_8).length);
                response.getWriter().write(voJson);
            }
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

    }
}
