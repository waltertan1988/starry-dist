package com.walter.starry.security.base.component.redis;

import com.walter.starry.security.base.config.properties.AppSecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 基础服务统一定义Redis Key
 * @Author: walter.tan
 * @DateTime: 2023-11-09 09:54:52
 */
@Component
public class InfraRedisKeys {
    private static final String KEY_PREFIX = "infra";

    @Autowired
    private AppSecurityProperties appSecurityProperties;

    /**
     * 资源项对应的权限码集合的Key
     * @param resItemCode 资源项编码
     * @return
     */
    public String getResourceItemAuthoritiesKey(String resItemCode) {
        return String.format("%s:resourceItem:authorities:%s", this.prefix(KeyType.BIZ), resItemCode);
    }

    /**
     * 清理用户已失效会话集的分布式锁
     * @return
     */
    public String getLockKeyForCleanUserExpiredSessions(){
        return String.format("%s:user:cleanUserExpiredSessions", this.prefix(KeyType.LOCK));
    }

    private String prefix(KeyType keyType){
        return String.format("%s:%s:%s", appSecurityProperties.getNamespacePrefix(), KEY_PREFIX, keyType.name().toLowerCase());
    }

    private enum KeyType {
        /** 业务值 */
        BIZ,

        /**分布式锁*/
        LOCK
    }
}
