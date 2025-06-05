package com.walter.starry.security.base.vo.request.function;

import com.walter.starry.security.base.entity.AclResourceItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-13 23:26:29
 */
@Data
public class SaveFunctionItemRequest {

    private Long id;

    @NotBlank(message = "功能项编码不能为空")
    private String code;

    @NotBlank(message = "功能项名称不能为空")
    private String name;

    @NotBlank(message = "功能项URL不能为空")
    private String pattern;

    @NotEmpty(message = "HTTP请求方法不能为空")
    private List<String> httpMethodList;

    @NotBlank(message = "上级功能组编码不能为空")
    private String parentGroupCode;

    @NotNull(message = "顺序不能为空")
    private Long seq;

    @NotNull(message = "配置项不能为空")
    private AclResourceItem.Config config;
}
