package com.walter.starry.security.base.component.pulsar;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.core.PulsarTopic;

/**
 * Pulsar Topic自动生成
 * 参考：https://docs.spring.io/spring-pulsar/docs/1.0.3/reference/reference/pulsar-admin.html#pulsar-auto-topic-creation
 *
 * @Author: walter.tan
 * @DateTime: 2024-03-27 14:55:59
 */
@Configuration
public class PulsarTopicConfig {

    @Bean
    public PulsarTopic partitionedTopic() {
        return PulsarTopic.builder("persistent://my-tenant/my-namespace/partitioned-topic").numberOfPartitions(3).build();
    }
}
