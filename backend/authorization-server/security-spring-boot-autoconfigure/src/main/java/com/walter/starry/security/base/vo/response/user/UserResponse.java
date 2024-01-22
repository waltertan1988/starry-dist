package com.walter.starry.security.base.vo.response.user;

import lombok.Data;

/**
 * @author: walter.tan
 * @datetime: 2023/9/27 16:44
 */
@Data
public class UserResponse {
    private String username;

    private String nickname;

    private String password;

    private Boolean accountExpired;

    private Boolean accountLocked;

    private Boolean credentialsExpired;

    private Boolean enabled;

    private Long createTime;

    private Long updateTime;
}
