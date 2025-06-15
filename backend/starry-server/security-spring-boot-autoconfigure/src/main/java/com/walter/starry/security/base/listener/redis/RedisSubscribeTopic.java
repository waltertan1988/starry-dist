package com.walter.starry.security.base.listener.redis;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;

import java.lang.annotation.*;

/**
 * Redis在PUB/SUB模式下的订阅主题（已废弃，由Pulsar代替）
 * @Author: walter.tan
 * @DateTime: 2023-10-14 14:07:21
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisSubscribeTopic {

    MessageTopicEnum value();
}
