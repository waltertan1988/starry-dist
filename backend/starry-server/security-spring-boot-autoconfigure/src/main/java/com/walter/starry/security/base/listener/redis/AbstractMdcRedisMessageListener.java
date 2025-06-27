package com.walter.starry.security.base.listener.redis;

import com.walter.starry.security.base.common.message.MdcMessageWrapper;
import com.walter.starry.security.base.util.JsonUtil;
import com.walter.starry.security.base.util.MdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.Objects;

/**
 * Redis MDC 监听器
 * @Author: walter.tan
 * @DateTime: 2025-06-27 16:29:59
 */
@Slf4j
public abstract class AbstractMdcRedisMessageListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern){
        String body = new String(message.getBody());

        MdcMessageWrapper mdcMessageWrapper = JsonUtil.toBean(body, MdcMessageWrapper.class);

        if(Objects.isNull(mdcMessageWrapper)){
            log.info("cannot resolve redis message: {}", body);
            return;
        }

        try{
            MdcUtil.setTraceId(mdcMessageWrapper.getTraceId());
            this.handle(mdcMessageWrapper.getMessage(), pattern);
        }finally {
            MdcUtil.removeTraceId();
        }
    }

    /**
     * 处理MDC解包后的消息体
     * @param message
     * @param pattern
     */
    public abstract void handle(String message, byte[] pattern);
}
