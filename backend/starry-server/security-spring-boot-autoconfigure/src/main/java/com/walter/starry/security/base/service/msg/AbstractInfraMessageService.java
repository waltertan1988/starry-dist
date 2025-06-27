package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.MdcMessageWrapper;
import com.walter.starry.security.base.config.properties.AppMsgProps;
import com.walter.starry.security.base.util.JsonUtil;
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
     * 转换为带有MDC信息的消息并发送广播消息
     * @param messageTopicEnum
     * @param msgObj
     * @return
     * @param <T>
     */
    public <T> String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, T msgObj) throws Exception {
        String message = JsonUtil.toJson(new MdcMessageWrapper(JsonUtil.toJson(msgObj)));
        try{
            return this.sendBroadcastMessage(messageTopicEnum, message);
        }catch (Throwable t){
            throw new Exception(String.format("sendBroadcastMessage fail. topic: %s, message: %s", messageTopicEnum.name(), message), t);
        }
    }

    /**
     * 发送广播消息
     * @param messageTopicEnum
     * @param message
     * @return
     */
    public abstract String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, String message);
}
