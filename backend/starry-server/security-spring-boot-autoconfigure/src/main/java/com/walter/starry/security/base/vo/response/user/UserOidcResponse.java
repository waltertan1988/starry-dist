package com.walter.starry.security.base.vo.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-04 09:22:48
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOidcResponse {
    private Long id;
    private String username;
    private String oidcRegistrationId;
    private String openId;
    private Boolean enabled;
    private Long createTimeTs;
    private Long updateTimeTs;
}
