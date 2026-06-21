package com.walter.starry.ai.mcp.server.remote;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: walter.tan
 * @datetime: 2026/6/13 10:13
 */
@Data
public class AclAuthorityItemRes implements Serializable {
    @JsonPropertyDescription("物理主键")
    private Long id;

    @JsonPropertyDescription("编码值")
    private String code;

    @JsonPropertyDescription("名称")
    private String name;

    @JsonPropertyDescription("上一层级权限的编码值")
    private String parentCode;

    @JsonPropertyDescription("优先级，数值越小，优先级越高")
    private Integer priority;

    @JsonPropertyDescription("是否为系统权限（即无法修改）。0-否，1-是")
    private Boolean systemAuthority;

    @JsonPropertyDescription("创建时间")
    private Date createTime;

    @JsonPropertyDescription("修改时间")
    private Date updateTime;
}
