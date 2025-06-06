package com.walter.starry.security.base.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: walter.tan
 * @datetime: 2023/9/18 13:24
 */
@Data
@Entity
@Table(name = "resource_group")
public class AclResourceGroup implements AclHierarchyResource, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    /** 资源组类型，参考：{@link TypeEnum} */
    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "seq", nullable = false)
    private Long seq;

    @Column(name = "parent_group_code", nullable = false)
    private String parentGroupCode;

    /** JSON配置，参考：{@link MenuConfig}或{@link FunctionConfig} */
    @Column(name = "config")
    private String config;

    @Column(name="create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name="update_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;

    @Getter
    @AllArgsConstructor
    public enum TypeEnum {
        /** 菜单 */
        MENU(1),

        /** 功能 */
        FUNCTION(2);

        private final Integer code;
    }

    /**
     * 菜单组配置
     */
    @Data
    public static class MenuConfig implements JsonConfig {
        /** 是否默认展开 */
        private Boolean defaultOpen = false;

        /** 图标 */
        private String icon;
    }

    /**
     * 功能组配置
     */
    @Data
    public static class FunctionConfig implements JsonConfig {
        /** 图标 */
        private String icon;
    }
}
