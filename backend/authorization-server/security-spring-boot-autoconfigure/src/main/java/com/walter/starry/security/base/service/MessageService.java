package com.walter.starry.security.base.service;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-16 11:19:47
 */
@Slf4j
@Service
public class MessageService {

    @Autowired
    @Qualifier("stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送广播消息（TODO 不建议使用Redis的PUB/SUB模式）
     * @param redisTopicEnum
     * @param message
     */
    public void publish(MessageTopicEnum redisTopicEnum, String message){
        stringRedisTemplate.convertAndSend(redisTopicEnum.name(), Objects.requireNonNull(message));
    }
}
