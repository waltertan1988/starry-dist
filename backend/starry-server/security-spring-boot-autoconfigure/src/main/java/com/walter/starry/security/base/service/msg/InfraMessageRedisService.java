package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.RedisMessage;
import com.walter.starry.security.base.config.RedisConfig;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 基础应用的Redis消息发送服务（兜底服务）
 * @Author: walter.tan
 * @DateTime: 2025-06-17 14:03:28
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.message.redis.enabled", havingValue = "true")
public class InfraMessageRedisService extends AbstractInfraMessageService {
    @Value("${app.message.redis.namespace}")
    private String namespace;
    @Autowired
    private RedisConfig redisConfig;
    @Autowired
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public <T> String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, T msgObj) {
        String channelName = redisConfig.getChannelName(namespace, messageTopicEnum.name());
        RedisMessage redisMessage = new RedisMessage(JsonUtil.toJson(msgObj));
        String message = JsonUtil.toJson(redisMessage);
        try{
            stringRedisTemplate.convertAndSend(channelName, message);
            return redisMessage.getMsgId();
        }catch (Throwable t){
            throw new RuntimeException(String.format("sendBroadcastMessage fail. channelName: %s, topic: %s, message: %s",
                    channelName, messageTopicEnum.name(), message), t);
        }
    }
}
