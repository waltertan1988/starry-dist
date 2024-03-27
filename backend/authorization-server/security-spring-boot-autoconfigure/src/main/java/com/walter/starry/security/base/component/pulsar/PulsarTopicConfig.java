package com.walter.starry.security.base.component.pulsar;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.config.properties.AppPulsarProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.core.PulsarTopic;
import org.springframework.util.Assert;

/**
 * Pulsar Topic自动生成（配置在消息生产者方）
 * 参考：https://docs.spring.io/spring-pulsar/docs/1.0.3/reference/reference/pulsar-admin.html#pulsar-auto-topic-creation
 *
 * @Author: walter.tan
 * @DateTime: 2024-03-27 14:55:59
 */
@Slf4j
@Configuration
public class PulsarTopicConfig implements InitializingBean {

    @Autowired
    private AppPulsarProperties appPulsarProperties;

    @Value("${spring.application.name}")
    private String namespace;

    @Bean
    public PulsarTopic roleChangeBroadcastPulsarTopic() {
        String topic = String.format(MessageTopicEnum.ROLE_CHANGE_BROADCAST.getPulsarTopic(), appPulsarProperties.getTenant(), namespace);
        log.info("creating pulsar topic: {}", topic);
        return PulsarTopic.builder(topic).build();
    }

    @Bean
    public PulsarTopic resourceChangeBroadcastPulsarTopic() {
        String topic = String.format(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST.getPulsarTopic(), appPulsarProperties.getTenant(), namespace);
        log.info("creating pulsar topic: {}", topic);
        return PulsarTopic.builder(topic).build();
    }

    @Override
    public void afterPropertiesSet() {
        Assert.hasText(namespace, "properties [spring.application.name] cannot be blank");
    }
}
