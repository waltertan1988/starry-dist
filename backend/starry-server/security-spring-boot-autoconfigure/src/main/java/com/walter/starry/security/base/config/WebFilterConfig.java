package com.walter.starry.security.base.config;

import com.walter.starry.security.base.component.mvc.MdcFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 配置Web Filter
 * @Author: walter.tan
 * @DateTime: 2025-06-26 17:13:44
 */
@Configuration
public class WebFilterConfig {
    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistrationBean() {
        FilterRegistrationBean<MdcFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new MdcFilter());
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }
}
