package com.walter.starry.security.base.component.scheduling;

import com.walter.starry.security.base.component.redis.RedisKeyComponent;
import com.walter.starry.security.base.component.redis.RedisLockTemplate;
import com.walter.starry.security.base.component.security.JpaUserDetailsService;
import com.walter.starry.security.base.config.properties.AppSchedulingCleanUserExpiredSessionsProperties;
import com.walter.starry.security.base.entity.AclUser;
import com.walter.starry.security.base.repository.AclUserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Session会话相关的定时任务
 * @Author: walter.tan
 * @DateTime: 2024-02-02 21:42:02
 */
@Slf4j
@Component
public class SessionScheduler {
    @Autowired
    private AppSchedulingCleanUserExpiredSessionsProperties properties;
    @Autowired
    private RedisKeyComponent redisKeyComponent;
    @Autowired
    private RedisLockTemplate redisLockTemplate;
    @Autowired
    private AclUserRepository aclUserRepository;
    @Autowired
    private JpaUserDetailsService jpaUserDetailsService;

    /**
     * 清理用户已失效的会话集
     */
    @Async("unboundedVirtualThreadTaskExecutor")
    @Scheduled(cron = "${app.scheduling.clean-user-expired-sessions.cron:-}")
    public void cleanUserExpiredSessions() {
        String lockKey = redisKeyComponent.getLockKeyForCleanUserExpiredSessions();
        redisLockTemplate.tryLockAndCallback(lockKey, 0L, 2 * properties.getRunDuration().toMillis(), TimeUnit.MILLISECONDS, null,
            () -> {
                Date runStartTime = new Date();
                final long expectedRunEndTimestamp = runStartTime.getTime() + properties.getRunDuration().toMillis();
                final Date cleanEndDate = DateUtils.addDays(runStartTime, -properties.getCleanFromDaysBefore());

                Pageable pageable = PageRequest.of(0, 100);
                Specification<AclUser> spec = (root, query, builder) -> {
                    List<Predicate> andPredicates = new ArrayList<>();
                    andPredicates.add(builder.lessThanOrEqualTo(root.get("expiredSessionsCleanTime").as(Date.class), cleanEndDate));
                    return builder.and(andPredicates.toArray(new Predicate[0]));
                };
                List<AclUser> userList = aclUserRepository.findAll(spec, pageable).getContent();
                while (CollectionUtils.isNotEmpty(userList)){
                    // TODO tyx 清理用户已失效的会话集

                    if(System.currentTimeMillis() > expectedRunEndTimestamp){
                        log.warn("cleanUserExpiredSessions is paused because it exceeded the running duration.");
                        break;
                    }
                    userList = aclUserRepository.findAll(spec, pageable).getContent();
                }
                return null;
            });
    }
}
