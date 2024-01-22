package com.walter.starry.security.base.vo.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * @author walter.tan
 */
@Data
public class SaveUserRequest {

    @Length(max = 128, message = "账号长度不能超过{max}")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Length(max = 255, message = "昵称长度不能超过{max}")
    private String nickname;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
