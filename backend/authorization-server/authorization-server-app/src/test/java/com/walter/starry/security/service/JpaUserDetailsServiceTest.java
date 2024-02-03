package com.walter.starry.security.service;

import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import com.walter.starry.security.base.component.scheduling.SessionScheduler;
import com.walter.starry.security.base.component.security.JpaUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: walter.tan
 * @DateTime: 2024-02-01 09:39:37
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
public class JpaUserDetailsServiceTest {

    @Autowired
    private JpaUserDetailsService jpaUserDetailsService;
    @Autowired
    private SessionScheduler sessionScheduler;

    @Test
    void cleanUserExpiredSessions() throws Exception {
        jpaUserDetailsService.cleanUserExpiredSessions("admin");
    }

    @Test
    void test() throws InterruptedException {
        sessionScheduler.cleanUserExpiredSessions();

        Thread.sleep(1000 * 3600);
    }
}
