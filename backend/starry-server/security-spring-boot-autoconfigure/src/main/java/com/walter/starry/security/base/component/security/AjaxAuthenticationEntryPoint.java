package com.walter.starry.security.base.component.security;

import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.vo.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 自定义匿名（含session过期）拒绝访问的处理器
 * @author: walter.tan
 * @datetime: 2023/9/14 22:44
 */
public class AjaxAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String json = JsonUtil.toJson(ApiResponse.fail(ApiResponse.ErrCode.UNAUTHORIZED, authException.getMessage()));
        response.setContentType("application/json;charset=UTF-8");
        if(StringUtils.hasText(json)){
            response.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
            response.getWriter().write(json);
        }
    }
}
