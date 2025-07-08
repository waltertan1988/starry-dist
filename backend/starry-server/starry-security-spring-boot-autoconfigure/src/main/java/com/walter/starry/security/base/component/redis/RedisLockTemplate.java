package com.walter.starry.security.base.component.redis;

import com.walter.starry.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁的操作模板
 * @Author: walter.tan
 * @DateTime: 2024-02-03 00:51:49
 */
@Slf4j
@Component
public class RedisLockTemplate {
    @Autowired
    private RedissonClient redissonClient;

    public <T> T tryLockAndCallback(String key, Long waitTime, Long leaseTime, TimeUnit timeUnit, Supplier<BizException> orElseThrow, Supplier<T> lockCallback) {
        Assert.notNull(lockCallback, "lockCallback cannot be null");

        RLock rLock = redissonClient.getLock(key);
        try {
            if (rLock.tryLock(waitTime, leaseTime, timeUnit)) {
                return lockCallback.get();
            }else if (Objects.nonNull(orElseThrow)){
                throw orElseThrow.get();
            }else{
                log.warn("tryLock fail. key:{}", key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("lock interrupted. key:{}", key);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
        return null;
    }
}
