package com.walter.starry.security.base.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: walter.tan
 * @datetime: 2023/9/18 13:24
 */
@Data
@Entity
@Table(name = "resource_item")
public class AclResourceItem implements AclHierarchyResource, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "http_method_list", nullable = false)
    private String httpMethodList;

    @Column(name = "pattern", nullable = false)
    private String pattern;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "seq", nullable = false)
    private Long seq;

    @Column(name = "parent_group_code", nullable = false)
    private String parentGroupCode;

    /** JSON配置，参考：{@link Config} */
    @Column(name = "config")
    private String config;

    @Column(name="create_time")
    private Date createTime;

    @Column(name="update_time")
    private Date updateTime;

    @Data
    public static class Config implements JsonConfig {
        /** 图标 */
        private String icon;
    }
}
