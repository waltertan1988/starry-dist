package com.walter.starry.security.base.bo.elasticsearch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2024-03-10 16:40:57
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsUser {

    private String username;

    private String nickname;

    @JsonProperty(value = "oidc_registration_id")
    private String oidcRegistrationId;

    @JsonProperty(value = "open_id")
    private String openId;

    @JsonProperty(value = "account_expired")
    private Boolean accountExpired;

    @JsonProperty(value = "account_locked")
    private Boolean accountLocked;

    @JsonProperty(value = "credentials_expired")
    private Boolean credentialsExpired;

    private Boolean enabled;

    @JsonProperty(value = "expired_sessions_clean_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "GMT+8")
    private Date expiredSessionsCleanTime;

    @JsonProperty(value = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "GMT+8")
    private Date createTime;

    @JsonProperty(value = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "GMT+8")
    private Date updateTime;

    private List<EsUserAuthority> authorities;

    @Data
    public static class EsUserAuthority {

        private Long id;

        private String authority;

        @JsonProperty(value = "create_time")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "GMT+8")
        private Date createTime;

        @JsonProperty(value = "update_time")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "GMT+8")
        private Date updateTime;
    }
}
