package com.walter.starry.security.base.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 应用的消息中间件配置
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

    /** Kafka消息配置 */
    private Kafka kafka;

    @Data
    public static class Redis{
        /** 是否启用Redis消息，如使用了其他消息中间件，建议禁用此选项 */
        private boolean enabled = false;
        /** 命名空间 */
        private String namespace = "";
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
        }
    }

    @Data
    public static class Kafka{
        /**
         * 基础服务使用的kafka集群key
         */
        private String infraClusterKey;
        /**
         * 各个Kafka集群的注册信息
         */
        private final Map<String, Cfg> cluster = new HashMap<>();

        public Cfg getClusterCfg(String clusterKey){
            return this.getCluster().get(clusterKey);
        }

        public Cfg getInfraClusterCfg(){
            return this.getClusterCfg(this.infraClusterKey);
        }

        @Data
        public static class Cfg{
            private String bootstrapServers;

            private Consumer consumer = new Consumer();

            private Producer producer = new Producer();

            @Data
            public static class Consumer{
                private Integer concurrency = 2;
                private Integer maxPollTimeOut = 30000;
                private Integer maxPollRecords = 1;
                private Boolean autoCommit = true;
                private String autoCommitInterval = "100";
                private String autoOffsetReset = "latest";
                private String sessionTimeout = "20000";
                private String shareAcknowledgementMode = "implicit";
            }

            @Data
            public static class Producer{
                private String retries = "1";
                private String batchSize = "16384";
                private String lingerMs = "1";
                private String bufferMemory = "1024000";
            }
        }
    }
}
