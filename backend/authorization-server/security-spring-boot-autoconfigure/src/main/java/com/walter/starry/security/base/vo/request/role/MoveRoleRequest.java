package com.walter.starry.security.base.vo.request.role;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-17 13:08:00
 */
@Data
public class MoveRoleRequest {

    /**
     * 待移动角色编码
     */
    @NotBlank(message = "待移动角色编码不能为空")
    private String code;

    /**
     * 移动到角色编码
     */
    @NotBlank(message = "移动到角色编码不能为空")
    private String moveToCode;
}
