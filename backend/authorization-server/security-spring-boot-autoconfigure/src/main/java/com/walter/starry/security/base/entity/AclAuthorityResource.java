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
@Table(name = "authority_resource")
public class AclAuthorityResource implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "resource_item_code", nullable = false)
    private String resourceItemCode;

    @Column(name = "authority_item_code", nullable = false)
    private String authorityItemCode;

    @Column(name="create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name="update_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
}
