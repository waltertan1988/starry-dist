package com.walter.starry.security.base.listener.redis;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.RedisMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 测试的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
@RedisSubscribe(namespace = "${app.message.redis.namespace}", topic = MessageTopicEnum.TEST_BROADCAST)
@ConditionalOnProperty(name = "app.message.redis.enabled", havingValue = "true")
public class TestBroadcastRedisListener extends AbstractMdcRedisMessageListener {
    @Override
    public void handle(RedisMessage message, byte[] pattern) {
        log.info("redis TEST_BROADCAST msgId: {}, body: {}", message.getMsgId(), message.getBody());
    }
}
