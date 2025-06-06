package com.walter.starry.security.base.component.security;

import com.walter.starry.security.base.bo.AclUserBo;
import com.walter.starry.security.base.bo.MergedOidcUser;
import com.walter.starry.security.base.util.HttpServletRequestUtil;
import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.vo.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 扩展支持Ajax登录成功处理器（参考：org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler）
 * @author: walter.tan
 * @datetime: 2023/9/3 23:16
 */
public class AjaxSupportedSavedRequestAwareAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public AjaxSupportedSavedRequestAwareAuthenticationSuccessHandler(String defaultTargetUrl){
        Assert.hasText(defaultTargetUrl, "defaultTargetUrl cannot be blank");
        super.setDefaultTargetUrl(defaultTargetUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        if(HttpServletRequestUtil.isAjaxRequest(request)){
            // AJAX登录
            String json = JsonUtil.toJson(ApiResponse.success(authentication));
            response.setContentType("application/json;charset=UTF-8");
            if(StringUtils.hasText(json)){
                response.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
                response.getWriter().write(json);
            }

            super.clearAuthenticationAttributes(request);
        }else {
            // 非AJAX登录
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // 此方法在使用非Ajax登录且无RequestCache的时候会调用，用于获取默认的重定向路径

        String targetUrl = super.determineTargetUrl(request, response, authentication);
        String prefix = targetUrl.contains("?") ? "&" : "?";

        String nickname = authentication.getName();;
        if(authentication.getPrincipal() instanceof AclUserBo bo){
            nickname = bo.getNickname();
        }else if(authentication.getPrincipal() instanceof MergedOidcUser mergedOidcUser){
            if(mergedOidcUser.getUserDetails() instanceof AclUserBo bo){
                nickname = bo.getNickname();
            }
        }
        String queryString = String.format("nickname=%s", URLEncoder.encode(nickname, StandardCharsets.UTF_8));
        return targetUrl + prefix + queryString;
    }
}
