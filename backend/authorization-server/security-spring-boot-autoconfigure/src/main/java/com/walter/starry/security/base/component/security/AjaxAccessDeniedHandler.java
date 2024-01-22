package com.walter.starry.security.base.component.security;

import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 自定义权限拒绝访问的处理器
 * @author: walter.tan
 * @datetime: 2023/9/14 21:50
 */
@Slf4j
public class AjaxAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (response.isCommitted()) {
            log.trace("Did not write to response since already committed");
            return;
        }

        // Put exception into request scope (perhaps of use to a view)
        request.setAttribute(WebAttributes.ACCESS_DENIED_403, accessDeniedException);

        String json = JsonUtil.toJson(ApiResponse.fail(ApiResponse.ErrCode.FORBIDDEN, accessDeniedException.getMessage()));
        response.setContentType("application/json;charset=UTF-8");
        if(StringUtils.hasText(json)){
            response.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
            response.getWriter().write(json);
        }
    }
}
