package com.walter.starry.security.base.listener.pulsar;

import com.walter.starry.common.core.message.MessageListenerPostProcessorChain;
import com.walter.starry.security.base.component.pulsar.PulsarTopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Pulsar消费者的切面类
 * @author walter.tan
 * @date 2023-06-16 9:08
 */
@Slf4j
@Aspect
@Component
@ConditionalOnBean(PulsarTopicConfig.class)
public class PulsarMessageListenerAspect {
    @Autowired
    private MessageListenerPostProcessorChain messageListenerPostProcessorChain;

    @Pointcut("@annotation(org.springframework.pulsar.annotation.PulsarListener)")
    public void annotationPointcut4PulsarMessageListener() { }

    /**
     * 处理MDC信息和消息体
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("annotationPointcut4PulsarMessageListener()")
    public Object handleMdcPulsarMessageListener(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return messageListenerPostProcessorChain.handle(() -> {
                try {
                    return joinPoint.proceed(joinPoint.getArgs());
                } catch (Throwable e) {
                    log.error("handleMdcPulsarMessageListener fail", e);
                    throw new RuntimeException(e);
                }
            }, joinPoint.getArgs());
        } catch (MessageListenerPostProcessorChain.MessageListenerPostProcessorChainException e) {
            throw new RuntimeException(e);
        }
    }
}