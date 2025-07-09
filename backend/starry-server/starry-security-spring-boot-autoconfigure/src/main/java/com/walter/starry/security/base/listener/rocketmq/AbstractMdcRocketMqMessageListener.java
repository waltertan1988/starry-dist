package com.walter.starry.security.base.listener.rocketmq;

import com.walter.starry.common.function.MessageListenerPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Redis MDC 监听器
 * @Author: walter.tan
 * @DateTime: 2025-06-27 16:29:59
 */
@Slf4j
public abstract class AbstractMdcRocketMqMessageListener implements RocketMQListener<MessageExt> {
    @Autowired(required = false)
    private List<MessageListenerPostProcessor> messageListenerPostProcessorList;

    @Override
    public void onMessage(MessageExt messageExt) {
        if(CollectionUtils.isNotEmpty(messageListenerPostProcessorList)){
            for (MessageListenerPostProcessor processor : messageListenerPostProcessorList) {
                processor.preHandle(messageExt);
            }
        }

        try{
            this.handle(messageExt);
        }finally {
            if(CollectionUtils.isNotEmpty(messageListenerPostProcessorList)){
                for (MessageListenerPostProcessor processor : messageListenerPostProcessorList.reversed()) {
                    try{
                        processor.postHandle(messageExt);
                    }catch (Throwable t){
                        log.error("MessageListenerPostProcessor postHandle fail", t);
                    }
                }
            }
        }
    }

    /**
     * 处理MDC解包后的消息体
     * @param messageExt
     */
    public abstract void handle(MessageExt messageExt);
}
