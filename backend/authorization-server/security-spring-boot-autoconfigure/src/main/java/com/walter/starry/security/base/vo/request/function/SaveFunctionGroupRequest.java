package com.walter.starry.security.base.vo.request.function;

import com.walter.starry.security.base.entity.AclResourceGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-13 23:26:29
 */
@Data
public class SaveFunctionGroupRequest {

    private Long id;

    @NotBlank(message = "功能组编码不能为空")
    private String code;

    @NotBlank(message = "功能组名称不能为空")
    private String name;

    @NotBlank(message = "上级功能组编码不能为空")
    private String parentGroupCode;

    @NotNull(message = "顺序不能为空")
    private Long seq;

    @NotNull(message = "配置项不能为空")
    private AclResourceGroup.FunctionConfig config;
}
