package com.walter.starry.security.base.listener.rocketmq;

import com.walter.starry.common.core.MessageListenerPostProcessorChain;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.CastUtils;

/**
 * Redis MDC 监听器
 * @Author: walter.tan
 * @DateTime: 2025-06-27 16:29:59
 */
@Slf4j
public abstract class AbstractRocketMqMessageListener implements RocketMQListener<MessageExt> {
    @Autowired
    private MessageListenerPostProcessorChain messageListenerPostProcessorChain;

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            messageListenerPostProcessorChain.handle(objects -> {
                AbstractRocketMqMessageListener.this.handle(CastUtils.cast(objects[0]));
                return null;
            }, messageExt);
        } catch (MessageListenerPostProcessorChain.MessageListenerPostProcessorChainException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理MDC解包后的消息体
     * @param messageExt
     */
    public abstract void handle(MessageExt messageExt);
}
