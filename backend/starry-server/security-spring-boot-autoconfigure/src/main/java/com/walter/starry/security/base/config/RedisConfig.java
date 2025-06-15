package com.walter.starry.security.base.config;

import com.walter.starry.security.base.listener.redis.RedisSubscribeTopic;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
     * 配置Redis消息监听
     * @param connectionFactory
     * @param executor
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "app.message.redis.enabled", havingValue = "true")
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

    /**
     * Redisson相关配置
     */
    @Slf4j
    @Configuration(proxyBeanMethods = false)
    public static class RedissonConfiguration implements AutoCloseable {
        private static final String CONFIG_FILE = "redisson.yml";

        private final RedissonClient redissonClient;

        public RedissonConfiguration() throws IOException {
            URL resource = RedissonConfiguration.class.getClassLoader().getResource(CONFIG_FILE);
            log.info("Reading redisson config file:{}", resource);
            Config config = Config.fromYAML(resource);
            redissonClient = Redisson.create(config);
        }

        @Bean
        public RedissonClient redissonClient() {
            return redissonClient;
        }

        @Override
        public void close() {
            this.redissonClient.shutdown(0, 60, TimeUnit.SECONDS);
            log.info("Shutdown redissonClient successfully.");
        }
    }
}
