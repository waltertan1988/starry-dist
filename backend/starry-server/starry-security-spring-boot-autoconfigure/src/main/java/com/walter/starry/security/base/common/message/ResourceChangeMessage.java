package com.walter.starry.security.base.common.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-14 12:21:12
 */
@Data
public class ResourceChangeMessage {
    /** 变更时间 */
    private Date changeTime;
    /** 变更前的数据 */
    private ResourceData before;
    /** 变更后的数据 */
    private ResourceData after;
    /** 权限变更的数据 */
    private ChangeAuthorityData changeAuthorityData;

    public static ResourceChangeMessage ofChangeAuthority(Date changeTime, ChangeAuthorityData changeAuthorityData){
        ResourceChangeMessage message = new ResourceChangeMessage();
        message.changeTime = changeTime;
        message.changeAuthorityData = changeAuthorityData;
        return message;
    }

    public static ResourceChangeMessage ofCreate(ResourceData data){
        ResourceChangeMessage message = new ResourceChangeMessage();
        message.changeTime = data.updateTime;
        message.after = data;
        return message;
    }

    public static ResourceChangeMessage ofUpdate(ResourceData before, ResourceData after){
        ResourceChangeMessage message = new ResourceChangeMessage();
        message.changeTime = after.updateTime;
        message.before = before;
        message.after = after;
        return message;
    }

    public static ResourceChangeMessage ofDelete(ResourceData data){
        ResourceChangeMessage message = new ResourceChangeMessage();
        message.changeTime = data.updateTime;
        message.before = data;
        return message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceData {
        private String code;
        private String name;
        private String httpMethodList;
        private String pattern;
        private Long seq;
        private Date updateTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeAuthorityData {
        private String resourceItemCode;
        private List<String> newRoleCodeList;
        private List<String> removeRoleCodeList;
    }
}
