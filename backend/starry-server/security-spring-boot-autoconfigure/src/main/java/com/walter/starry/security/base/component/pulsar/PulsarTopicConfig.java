package com.walter.starry.security.base.component.pulsar;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.config.properties.AppMsgProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.core.PulsarTopic;
import org.springframework.pulsar.core.PulsarTopicBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Pulsar Topic自动生成（配置在消息生产者方）
 * 参考：https://docs.spring.io/spring-pulsar/docs/1.0.3/reference/reference/pulsar-admin.html#pulsar-auto-topic-creation
 *
 * @Author: walter.tan
 * @DateTime: 2024-03-27 14:55:59
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.message.pulsar.base-reg.namespace")
public class PulsarTopicConfig implements InitializingBean {

    @Autowired
    private AppMsgProps appMsgProps;

    @Bean
    public PulsarTopic roleChangeBroadcastBasePulsarTopic() {
        String topic = String.format(MessageTopicEnum.ROLE_CHANGE_BROADCAST.getPulsar().getTopic(),
                appMsgProps.getPulsar().getBaseReg().getTenant(), appMsgProps.getPulsar().getBaseReg().getNamespace());
        log.info("creating pulsar topic: {}", topic);
        return new PulsarTopicBuilder().name(topic).build();
    }

    @Bean
    public PulsarTopic resourceChangeBroadcastBasePulsarTopic() {
        String topic = String.format(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST.getPulsar().getTopic(),
                appMsgProps.getPulsar().getBaseReg().getTenant(), appMsgProps.getPulsar().getBaseReg().getNamespace());
        log.info("creating pulsar topic: {}", topic);
        return new PulsarTopicBuilder().name(topic).build();
    }

    @Override
    public void afterPropertiesSet() {
        boolean flag = Objects.nonNull(appMsgProps.getPulsar().getBaseReg())
                && StringUtils.hasText(appMsgProps.getPulsar().getBaseReg().getTenant())
                && StringUtils.hasText(appMsgProps.getPulsar().getBaseReg().getNamespace());
        Assert.isTrue(flag, "properties [app.pulsar.base-reg.tenant] and [app.pulsar.base-reg.namespace] should be setup");
    }
}
