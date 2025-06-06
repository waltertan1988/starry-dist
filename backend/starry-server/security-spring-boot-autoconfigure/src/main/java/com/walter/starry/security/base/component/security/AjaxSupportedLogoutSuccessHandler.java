package com.walter.starry.security.base.component.security;

import com.walter.starry.security.base.vo.response.ApiResponse;
import com.walter.starry.security.base.util.HttpServletRequestUtil;
import com.walter.starry.security.base.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 支持Ajax与非Ajax的退出登录成功处理器（参考默认的处理器：{@link SimpleUrlLogoutSuccessHandler}）
 * @author: walter.tan
 * @datetime: 2023/9/24 21:04
 */
public class AjaxSupportedLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    protected final Log logger = LogFactory.getLog(this.getClass());

    public AjaxSupportedLogoutSuccessHandler(){
        this(DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?logout");
    }

    public AjaxSupportedLogoutSuccessHandler(String logoutSuccessUrl){
        super.setDefaultTargetUrl(logoutSuccessUrl);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            this.logger.debug(LogMessage.format("Did not redirect to %s since response already committed."));
            return;
        }

        if(HttpServletRequestUtil.isAjaxRequest(request)){
            String voJson = JsonUtil.toJson(ApiResponse.success());
            response.setContentType("application/json;charset=UTF-8");
            if(StringUtils.hasText(voJson)){
                response.setContentLength(voJson.getBytes(StandardCharsets.UTF_8).length);
                response.getWriter().write(voJson);
            }
        }else{
            super.onLogoutSuccess(request, response, authentication);
        }
    }
}
