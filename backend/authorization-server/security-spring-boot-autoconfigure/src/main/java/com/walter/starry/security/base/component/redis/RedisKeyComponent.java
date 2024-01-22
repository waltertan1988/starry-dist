package com.walter.starry.security.base.component.redis;

import com.walter.starry.security.base.config.properties.AppSecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 统一定义Redis Key
 * @Author: walter.tan
 * @DateTime: 2023-11-09 09:54:52
 */
@Component
public class RedisKeyComponent {
    @Autowired
    private AppSecurityProperties appSecurityProperties;

    /**
     * 资源项对应的权限码集合的Key
     * @param resItemCode 资源项编码
     * @return
     */
    public String getResourceItemAuthoritiesKey(String resItemCode) {
        return String.format("%s:resource:item:authorities:%s", appSecurityProperties.getNamespacePrefix(), resItemCode);
    }
}
