package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.config.properties.AppMsgProps;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

/**
 * 定义基础应用的消息发送服务
 * @Author: walter.tan
 * @DateTime: 2025-06-17 13:56:22
 */
public abstract class AbstractInfraMessageService implements Ordered {
    @Autowired
    protected AppMsgProps appMsgProps;

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    /**
     * 发送广播消息
     * @param messageTopicEnum
     * @param message
     * @return
     */
    @Nullable
    public abstract String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, String message);
}
