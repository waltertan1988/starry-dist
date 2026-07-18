package com.walter.starry.security.base.component;

import com.walter.starry.business.app.controller.AbstractControllerJupiterTest;
import com.walter.starry.security.base.component.redis.RedisLockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisLockTemplateTest extends AbstractControllerJupiterTest {
    @Autowired
    private RedisLockTemplate redisLockTemplate;

    @Test
    void tryLockAndCallback(){

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<CompletableFuture<?>> futureList = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                redisLockTemplate.tryLockAndCallback("testLock", 0L, 1L, TimeUnit.MINUTES, null, () -> {
                    try {
                        log.info(">>>>>> lock start...");
                        TimeUnit.MINUTES.sleep(1);
                        log.info(">>>>>> lock end...");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            }, executorService);
            futureList.add(future);
        }

        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
    }
}
