package com.walter.starry.security.base.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: walter.tan
 * @datetime: 2023/9/26 22:28
 */
@Data
@Entity
@Table(name = "users")
public class AclUser implements Serializable {
    @Id
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "oidc_registration_id")
    private String oidcRegistrationId;

    @Column(name = "open_id")
    private String openId;

    @Column(name = "account_expired", nullable = false)
    private Boolean accountExpired;

    @Column(name = "account_locked", nullable = false)
    private Boolean accountLocked;

    @Column(name = "credentials_expired", nullable = false)
    private Boolean credentialsExpired;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name="expired_sessions_clean_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiredSessionsCleanTime;

    @Column(name="create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name="update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
}
