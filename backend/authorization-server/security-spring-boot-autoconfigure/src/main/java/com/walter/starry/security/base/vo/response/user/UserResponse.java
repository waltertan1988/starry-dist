package com.walter.starry.security.base.vo.response.user;

import lombok.Data;

/**
 * @author: walter.tan
 * @datetime: 2023/9/27 16:44
 */
@Data
public class UserResponse {

    private Long id;

    private String username;

    private String nickname;

    private String password;

    private String oidcRegistrationId;

    private String openId;

    private Boolean accountExpired;

    private Boolean accountLocked;

    private Boolean credentialsExpired;

    private Boolean enabled;

    private Long expiredSessionsCleanTime;

    private Long createTime;

    private Long updateTime;
}
