package com.walter.starry.security.base.vo.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-10 14:35:05
 */
@Data
public class GrantAuthorityRequest {
    /**
     * 待授权的目标账号
     */
    @NotBlank(message = "待授权的目标账号不能为空")
    private String username;
    /**
     * 待添加的角色列表
     */
    private List<String> newRoleCodeList;
    /**
     * 待移除的角色列表
     */
    private List<String> removeRoleCodeList;
}
