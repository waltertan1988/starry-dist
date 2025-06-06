package com.walter.starry.security.base.component.security;

import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.util.HttpServletRequestUtil;
import com.walter.starry.security.base.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 支持Ajax与非Ajax的登录失败处理器
 * @author: walter.tan
 * @datetime: 2023/9/3 17:16
 */
public class AjaxSupportedAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public AjaxSupportedAuthenticationFailureHandler(String defaultFailureUrl){
        super(defaultFailureUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if(HttpServletRequestUtil.isAjaxRequest(request)){
            String json = JsonUtil.toJson(ApiResponse.fail(ApiResponse.ErrCode.BAD_REQUEST, exception.getMessage()));
            response.setContentType("application/json;charset=UTF-8");
            if(StringUtils.hasText(json)){
                response.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
                response.getWriter().write(json);
            }
        }else {
            super.onAuthenticationFailure(request, response, exception);
        }
    }
}
