package com.walter.starry.common.constant;

public interface MqConstant {

    interface Redis{
        String CONDITIONAL_ON_PROP = "app.message.redis.enabled";
    }

    interface Pulsar{
        String CONDITIONAL_ON_PROP_1 = "spring.pulsar.client.service-url";
        String CONDITIONAL_ON_PROP_2 = "spring.pulsar.admin.service-url";
    }

    interface RocketMq{
        String CONDITIONAL_ON_PROP = "rocketmq.name-server";
        String TOPIC_INFRA_BROADCAST = "INFRA_BROADCAST";
    }

    interface Kafka{
        String CONDITIONAL_ON_PROP = "app.message.kafka.base-cluster-key";
        String HEADER_KEY_MSG_ID = "msgId";
        String TOPIC_INFRA_BROADCAST = "%s.infra-broadcast";
    }
}
