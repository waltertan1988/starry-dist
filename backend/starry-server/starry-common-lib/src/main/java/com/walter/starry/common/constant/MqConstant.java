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

    }

    interface Kafka{

    }
}
