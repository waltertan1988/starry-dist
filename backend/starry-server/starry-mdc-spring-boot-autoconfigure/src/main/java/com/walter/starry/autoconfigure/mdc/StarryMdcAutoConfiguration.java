package com.walter.starry.autoconfigure.mdc;

import com.walter.starry.autoconfigure.mdc.web.MdcWebFilter;
import com.walter.starry.common.util.MdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import reactor.core.scheduler.Schedulers;

/**
 * @Author: walter.tan
 * @DateTime: 2023-12-12 23:19:02
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {"com.walter.starry.autoconfigure.mdc"})
public class StarryMdcAutoConfiguration {

    static {
        try {
            // 对reactor线程池，注册MDC特性
            Class.forName("reactor.core.scheduler.Schedulers");
            Schedulers.onScheduleHook("mdcHook", r -> MdcUtil.toMdcRunnable(MdcUtil.getTraceId(), r));
        } catch (ClassNotFoundException e) {
            log.warn("MDC for reactor is skipped");
        }
    }

    /**
     * 注册MdcWebFilter
     * @return
     */
    @Bean
    @ConditionalOnClass(OncePerRequestFilter.class)
    public FilterRegistrationBean<MdcWebFilter> mdcFilterRegistrationBean() {
        log.info("registering web filter: {}", MdcWebFilter.class.getName());
        FilterRegistrationBean<MdcWebFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new MdcWebFilter());
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }

    // TODO 注册虚拟线程池
}
