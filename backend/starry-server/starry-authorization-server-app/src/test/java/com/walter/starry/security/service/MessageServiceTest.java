package com.walter.starry.security.service;

import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.config.properties.AppMsgProps;
import com.walter.starry.security.base.service.MessageService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: walter.tan
 * @DateTime: 2024-03-28 15:18:45
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
public class MessageServiceTest {
    @Autowired
    private MessageService messageService;
    @Autowired
    private AppMsgProps appMsgProps;

    @Nested
    class PulsarTest {

        @Test
        void publishToPulsar() {
            String tenant = appMsgProps.getPulsar().getBaseReg().getTenant();
            String namespace = appMsgProps.getPulsar().getBaseReg().getNamespace();
            String messageId = messageService.publishToPulsar(MessageTopicEnum.ROLE_CHANGE_BROADCAST, tenant, namespace, "Hello Pulsar!");
            System.out.println(">>>>>>messageId: " + messageId);
        }
    }
}
