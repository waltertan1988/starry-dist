package com.walter.starry.security.base.vo.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-04 15:38:43
 */
@Data
public class ChangeUserOidcEnabledRequest {
    @NotBlank(message = "oidcRegistrationId不能为空")
    private String oidcRegistrationId;

    @NotBlank(message = "openId不能为空")
    private String openId;

    @NotNull(message = "是否启用")
    private Boolean enabled;
}
