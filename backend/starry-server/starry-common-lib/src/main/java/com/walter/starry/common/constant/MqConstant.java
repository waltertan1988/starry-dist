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
        String CONDITIONAL_ON_PROP = "app.message.kafka.infra-cluster-key";
        String HEADER_KEY_MSG_ID = "msgId";
        String INFRA_BROADCAST_TOPIC = "%s.infra-broadcast";

        String BEAN_NAME_INFRA_KAFKA_LISTENER_CONTAINER_FACTORY = "infraKafkaListenerContainerFactory";
        String BEAN_NAME_INFRA_SHARE_KAFKA_LISTENER_CONTAINER_FACTORY = "infraShareKafkaListenerContainerFactory";
        String BEAN_NAME_INFRA_KAFKA_TEMPLATE = "infraKafkaTemplate";
    }
}
