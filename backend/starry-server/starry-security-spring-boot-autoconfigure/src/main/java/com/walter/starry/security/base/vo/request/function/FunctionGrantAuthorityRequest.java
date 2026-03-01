package com.walter.starry.security.base.vo.request.function;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-13 23:26:29
 */
@Data
public class FunctionGrantAuthorityRequest {
    /** 功能项编码 */
    @NotBlank(message = "功能项编码不能为空")
    private String functionItemCode;

    /** 待添加的角色集合 */
    private List<String> newRoleCodeList;

    /** 待移除的角色集合 */
    private List<String> removeRoleCodeList;
}
