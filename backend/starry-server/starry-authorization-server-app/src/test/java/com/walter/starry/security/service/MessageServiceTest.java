package com.walter.starry.security.service;

import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.service.msg.InfraMessageService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

/**
 * @Author: walter.tan
 * @DateTime: 2024-03-28 15:18:45
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
public class MessageServiceTest {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private InfraMessageService infraMessageService;

    @Test
    void sendBroadcastMessage() throws InterruptedException {
        String resp = infraMessageService.sendBroadcastMessage(MessageTopicEnum.TEST_BROADCAST, "Hello World!");
        log.info("resp: {}", resp);

        TimeUnit.SECONDS.sleep(10);
    }
}
