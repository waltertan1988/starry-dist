package com.walter.starry.security.base.vo.request.role;

import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-11 10:18:55
 */
@Data
public class ListTreeRequest {
    /**
     * 角色编码
     */
    private String code;
    /**
     * 角色名称
     */
    private String name;

    /**
     * 是否为系统权限
     */
    private Boolean systemAuthority;
}
