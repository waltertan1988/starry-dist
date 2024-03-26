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
    ROLE_CHANGE_BROADCAST,
    /**资源变更 广播*/
    RESOURCE_CHANGE_BROADCAST
    ;

    public String toHyphenLowerCase(){
        return this.name().toLowerCase().replace('_', '-');
    }
}
