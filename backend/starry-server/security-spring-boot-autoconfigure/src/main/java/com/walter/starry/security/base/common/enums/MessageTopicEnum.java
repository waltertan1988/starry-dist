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
    /** 角色变更 广播 */
    ROLE_CHANGE_BROADCAST(new Pulsar("persistent://%s/%s/role-change-broadcast", 0)),
    /**资源变更 广播*/
    RESOURCE_CHANGE_BROADCAST(new Pulsar("persistent://%s/%s/resource-change-broadcast", 0))
    ;

    /** Pulsar信息 */
    private final Pulsar pulsar;

    @Getter
    @AllArgsConstructor
    public static class Pulsar {
        private String topic;
        private Integer topicPartitionNum;
    }
}
