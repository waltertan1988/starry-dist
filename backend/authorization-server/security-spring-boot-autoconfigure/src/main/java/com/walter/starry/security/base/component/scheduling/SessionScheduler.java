package com.walter.starry.security.base.component.scheduling;

import com.walter.starry.security.base.config.properties.AppSchedulingClearUserExpiredSessionsProperties;
import lombok.extern.slf4j.Slf4j;
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
@Async("unboundedVirtualThreadTaskExecutor")
public class SessionScheduler {
    @Autowired
    private AppSchedulingClearUserExpiredSessionsProperties properties;

    /**
     * 清理用户已失效的会话集
     */
    @Scheduled(cron = "${app.scheduling.clear-user-expired-sessions.cron:-}")
    public void clearUserExpiredSessions() {
        log.info("clearUserExpiredSessions for {}", properties.getRunDuration().toMinutes());
    }
}
