package com.walter.starry.security.base.listener.redis;

import com.walter.starry.common.core.MessageListenerPostProcessorChain;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.vo.RedisMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.util.CastUtils;

import java.util.Objects;

/**
 * Redis消息监听器
 * @Author: walter.tan
 * @DateTime: 2025-06-27 16:29:59
 */
@Slf4j
public abstract class AbstractRedisMessageListener implements MessageListener {
    @Autowired
    private MessageListenerPostProcessorChain messageListenerPostProcessorChain;

    @Override
    public void onMessage(Message message, byte[] pattern){
        String body = new String(message.getBody());

        RedisMessage redisMessage = JsonUtil.toBean(body, RedisMessage.class);

        if(Objects.isNull(redisMessage)){
            log.info("cannot resolve redis message: {}", body);
            return;
        }

        try {
            messageListenerPostProcessorChain.handle(objects -> {
                AbstractRedisMessageListener.this.handle(CastUtils.cast(objects[0]), CastUtils.cast(objects[1]));
                return null;
            }, redisMessage, pattern);
        } catch (MessageListenerPostProcessorChain.MessageListenerPostProcessorChainException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理消息体
     * @param message
     * @param pattern
     */
    public abstract void handle(RedisMessage message, byte[] pattern);
}
