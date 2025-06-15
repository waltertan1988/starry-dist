package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 应用的Pulsar配置
 * @Author: walter.tan
 * @DateTime: 2024-03-27 22:33:50
 */
@Data
@Configuration
@ConfigurationProperties("app.message")
public class AppMsgProps {
    /** Redis消息配置 */
    private Redis redis;

    /** Pulsar消息配置 */
    private Pulsar pulsar;

    @Data
    public static class Redis {
        /** 是否启用Redis消息 */
        private boolean enabled = false;
    }

    @Data
    public static class Pulsar {
        /**
         * 本应用所需的pulsar注册信息
         */
        private Reg baseReg;

        /**
         * 其他应用的pulsar注册信息
         */
        private final Map<String, Reg> regs = new HashMap<>();

        @Data
        public static class Reg {
            /**
             * pulsar租户
             */
            private String tenant;
            /**
             * pulsar命名空间
             */
            private String namespace;

            public boolean enabled(){
                return StringUtils.isNotBlank(namespace);
            }
        }
    }
}
