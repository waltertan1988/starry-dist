package com.walter.starry.security.base.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息主题
 * @Author: walter.tan
 * @DateTime: 2023-10-14 12:08:14
 */
@Getter
@AllArgsConstructor
public enum MessageTopicEnum {
    /** 测试 广播 */
    TEST_BROADCAST(
            new RocketMq("TEST"),
            new Pulsar("persistent://%s/%s/test-broadcast", 0),
            new Kafka("test-broadcast")),
    /** 角色变更 广播 */
    ROLE_CHANGE_BROADCAST(
            new RocketMq("ROLE_CHANGE"),
            new Pulsar("persistent://%s/%s/role-change-broadcast", 0),
            new Kafka("role-change-broadcast")),
    /**资源变更 广播*/
    RESOURCE_CHANGE_BROADCAST(
            new RocketMq("RESOURCE_CHANGE"),
            new Pulsar("persistent://%s/%s/resource-change-broadcast", 0),
            new Kafka("resource-change-broadcast")),
    ;

    /** RocketMq信息 */
    private final RocketMq rocketMq;

    /** Pulsar信息 */
    private final Pulsar pulsar;

    /** Kafka信息 */
    private final Kafka kafka;

    @Getter
    @AllArgsConstructor
    public static class RocketMq{
        private String tags;
    }

    @Getter
    @AllArgsConstructor
    public static class Pulsar {
        private String topic;
        private Integer topicPartitionNum;
    }

    @Getter
    @AllArgsConstructor
    public static class Kafka{
        private String topic;
    }
}
