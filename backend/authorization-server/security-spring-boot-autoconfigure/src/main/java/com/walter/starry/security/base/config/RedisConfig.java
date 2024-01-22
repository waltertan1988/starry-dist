package com.walter.starry.security.base.config;

import com.walter.starry.security.base.listener.annotation.RedisSubscribeTopic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * @author: walter.tan
 * @datetime: 2023/9/6 22:32
 */
@Slf4j
@Configuration
public class RedisConfig {
    @Autowired
    private List<MessageListener> messageListenerList;

    @Bean("stringRedisTemplate")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory){
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.setConnectionFactory(factory);
        return template;
    }

    /**
     * 配置Redis消息监听（TODO 不建议使用Redis的PUB/SUB模式）
     * @param connectionFactory
     * @param executor
     * @return
     */
    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory, @Qualifier("redisSubscribeVirtualThreadTaskExecutor") ExecutorService executor) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(executor);

        for (MessageListener messageListener : messageListenerList) {
            RedisSubscribeTopic redisSubscribeTopic = AnnotationUtils.findAnnotation(messageListener.getClass(), RedisSubscribeTopic.class);
            if(Objects.isNull(redisSubscribeTopic)){
                log.warn("@RedisSubscribeTopic is missing and skip adding MessageListener: {}", messageListener.getClass().getName());
                continue;
            }
            container.addMessageListener(messageListener, ChannelTopic.of(redisSubscribeTopic.value().name()));
        }

        return container;
    }
}
