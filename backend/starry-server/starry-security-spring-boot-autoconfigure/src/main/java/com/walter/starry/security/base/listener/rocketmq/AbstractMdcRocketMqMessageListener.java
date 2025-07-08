package com.walter.starry.security.base.listener.rocketmq;

import com.walter.starry.common.util.MdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;

/**
 * Redis MDC 监听器
 * @Author: walter.tan
 * @DateTime: 2025-06-27 16:29:59
 */
@Slf4j
public abstract class AbstractMdcRocketMqMessageListener implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt messageExt) {
        try{
            MdcUtil.setTraceId(messageExt.getUserProperty(MdcUtil.ATTR_TRACE_ID));
            this.handle(messageExt);
        }finally {
            MdcUtil.removeTraceId();
        }
    }

    /**
     * 处理MDC解包后的消息体
     * @param messageExt
     */
    public abstract void handle(MessageExt messageExt);
}
