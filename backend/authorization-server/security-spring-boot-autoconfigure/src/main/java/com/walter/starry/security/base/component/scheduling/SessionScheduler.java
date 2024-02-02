package com.walter.starry.security.base.component.scheduling;

import lombok.extern.slf4j.Slf4j;
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

    /**
     * 清理用户已失效的会话集
     */
    @Async("unboundedVirtualThreadTaskExecutor")
    @Scheduled(cron = "${app.scheduling.clear-user-expired-sessions}")
    public void clearUserExpiredSessions() {
        log.info("clearUserExpiredSessions executed");
    }
}
