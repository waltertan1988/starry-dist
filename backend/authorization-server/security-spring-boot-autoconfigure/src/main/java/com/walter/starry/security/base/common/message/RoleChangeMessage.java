package com.walter.starry.security.base.common.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-14 12:21:12
 */
@Data
public class RoleChangeMessage {
    /** 变更时间 */
    private Date changeTime;
    /** 变更前的数据 */
    private RoleData before;
    /** 变更后的数据 */
    private RoleData after;

    public static RoleChangeMessage ofCreate(RoleData roleData){
        RoleChangeMessage message = new RoleChangeMessage();
        message.changeTime = roleData.updateTime;
        message.before = null;
        message.after = roleData;
        return message;
    }

    public static RoleChangeMessage ofUpdate(RoleData before, RoleData after){
        RoleChangeMessage message = new RoleChangeMessage();
        message.changeTime = after.updateTime;
        message.before = before;
        message.after = after;
        return message;
    }

    public static RoleChangeMessage ofDelete(RoleData roleData){
        RoleChangeMessage message = new RoleChangeMessage();
        message.changeTime = roleData.updateTime;
        message.before = roleData;
        message.after = null;
        return message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleData {
        private String code;
        private String name;
        private String parentCode;
        private Date updateTime;
    }
}
