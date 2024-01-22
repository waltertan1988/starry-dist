package com.walter.starry.security.base.vo.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-13 23:26:29
 */
@Data
public class SaveRoleRequest {

    private Long id;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    private String parentCode;

    @NotNull(message = "顺序不能为空")
    private Integer priority;

    @NotNull(message = "系统权限标识不能为空")
    private Boolean systemAuthority;
}
