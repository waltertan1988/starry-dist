package com.walter.starry.security.base.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Elasticsearch索引别名枚举
 * @Author: walter.tan
 * @DateTime: 2024-03-10 17:33:17
 */
@Getter
@AllArgsConstructor
public enum ElasticsearchIndexAliasEnum {

    /** 用户信息索引 */
    USER("authorization_server.user", new String[]{"authorization_server.user.v1"}),
    ;

    /**
     * 索引别名
     */
    private final String alias;

    /**
     * 真实索引列表
     */
    private final String[] index;
}
