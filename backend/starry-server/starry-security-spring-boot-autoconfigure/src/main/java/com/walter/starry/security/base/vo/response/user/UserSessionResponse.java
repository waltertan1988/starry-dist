package com.walter.starry.security.base.vo.response.user;

import lombok.Data;

@Data
public class UserSessionResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 上次访问时间
     */
    private Long lastAccessedTime;

    /**
     * 失效时间间隔（毫秒）
     */
    private Long maxInactiveInterval;

    /**
     * 是否已失效
     */
    private Boolean expired;
}
