package com.walter.starry.security.base.component.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 自定义检查受CSRF保护URL的RequestMatcher
 * @author: walter.tan
 * @datetime: 2023/9/28 14:52
 */
public class RequireCsrfProtectionRequestMatcher implements RequestMatcher {

    private final List<String> urlList;

    public RequireCsrfProtectionRequestMatcher(List<String> urlList) {
        Assert.notNull(urlList, "urlList cannot be null");
        this.urlList = urlList;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        String path = request.getServletPath();
        return CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request) && urlList.contains(path);
    }
}
