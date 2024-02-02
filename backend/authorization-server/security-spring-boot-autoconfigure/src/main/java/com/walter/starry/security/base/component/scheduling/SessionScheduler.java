package com.walter.starry.security.base.component.scheduling;

import com.walter.starry.security.base.component.redis.RedisKeyComponent;
import com.walter.starry.security.base.component.security.JpaUserDetailsService;
import com.walter.starry.security.base.config.properties.AppSchedulingClearUserExpiredSessionsProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Session会话相关的定时任务
 * @Author: walter.tan
 * @DateTime: 2024-02-02 21:42:02
 */
@Slf4j
@Component
public class SessionScheduler {
    @Autowired
    private AppSchedulingClearUserExpiredSessionsProperties properties;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisKeyComponent redisKeyComponent;
    @Autowired
    private JpaUserDetailsService jpaUserDetailsService;

    /**
     * 清理用户已失效的会话集
     */
    @Async("unboundedVirtualThreadTaskExecutor")
    @Scheduled(cron = "${app.scheduling.clear-user-expired-sessions.cron:-}")
    public void clearUserExpiredSessions() {
        // TODO tyx 清理用户已失效的会话集
        log.info("clearUserExpiredSessions for {}", properties.getRunDuration().toMinutes());
    }
}
