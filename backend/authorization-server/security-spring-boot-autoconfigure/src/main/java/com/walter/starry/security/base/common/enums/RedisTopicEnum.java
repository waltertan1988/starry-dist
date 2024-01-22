package com.walter.starry.security.base.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Redis消息主题
 * @Author: walter.tan
 * @DateTime: 2023-10-14 12:08:14
 */
@Getter
@AllArgsConstructor
public enum RedisTopicEnum {
    /** 角色变更 广播 */
    ROLE_CHANGE_BROADCAST,
    /**资源变更 广播*/
    RESOURCE_CHANGE_BROADCAST

}
