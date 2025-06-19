package com.walter.starry.security.base.listener.redis;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 测试的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
@RedisSubscribeTopic(MessageTopicEnum.TEST_BROADCAST)
@ConditionalOnProperty(name = "app.message.redis.enabled", havingValue = "true")
public class TestBroadcaseRedisListener implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.info("redis TEST_BROADCAST message: {}", body);
    }
}
