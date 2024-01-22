package com.walter.starry.security.base.listener.annotation;

import com.walter.starry.security.base.common.enums.RedisTopicEnum;

import java.lang.annotation.*;

/**
 * Redis在PUB/SUB模式下的订阅主题（TODO 不建议使用Redis的PUB/SUB模式）
 * @Author: walter.tan
 * @DateTime: 2023-10-14 14:07:21
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisSubscribeTopic {

    RedisTopicEnum value();
}
