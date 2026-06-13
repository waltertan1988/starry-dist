package com.walter.starry.ai.mcp.server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: walter.tan
 * @datetime: 2026/6/13 10:13
 */
@Data
@Entity
@Table(name = "authority_item")
public class AclAuthorityItem implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "system_authority", nullable = false)
    private Boolean systemAuthority;

    @Column(name="create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name="update_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
}
