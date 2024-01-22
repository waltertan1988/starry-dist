package com.walter.starry.security.base.vo.request.menu;

import com.walter.starry.security.base.entity.AclResourceItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-13 23:26:29
 */
@Data
public class SaveMenuItemRequest {

    private Long id;

    @NotBlank(message = "菜单项编码不能为空")
    private String code;

    @NotBlank(message = "菜单项名称不能为空")
    private String name;

    @NotBlank(message = "菜单项URL不能为空")
    private String pattern;

    @NotBlank(message = "上级菜单组编码不能为空")
    private String parentGroupCode;

    @NotNull(message = "顺序不能为空")
    private Long seq;

    @NotNull(message = "配置项不能为空")
    private AclResourceItem.Config config;
}
