package com.walter.starry.security.base.listener.rocketmq;

import com.walter.starry.security.base.common.message.MdcMessageWrapper;
import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.util.MdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Redis MDC 监听器
 * @Author: walter.tan
 * @DateTime: 2025-06-27 16:29:59
 */
@Slf4j
public abstract class AbstractMdcRocketMqMessageListener implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);

        MdcMessageWrapper mdcMessageWrapper = JsonUtil.toBean(body, MdcMessageWrapper.class);

        if(Objects.isNull(mdcMessageWrapper)){
            log.info("cannot resolve redis message: {}", body);
            return;
        }

        try{
            MdcUtil.setTraceId(mdcMessageWrapper.getTraceId());
            this.handle(mdcMessageWrapper.getMessage(), messageExt);
        }finally {
            MdcUtil.removeTraceId();
        }
    }

    /**
     * 处理MDC解包后的消息体
     * @param message
     * @param messageExt
     */
    public abstract void handle(String message, MessageExt messageExt);
}
