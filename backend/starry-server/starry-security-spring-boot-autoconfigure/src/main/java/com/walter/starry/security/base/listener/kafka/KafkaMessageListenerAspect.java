package com.walter.starry.security.base.listener.kafka;

import com.walter.starry.common.constant.MqConstant;
import com.walter.starry.common.core.message.MessageListenerPostProcessorChain;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka消费者的切面类
 * @author walter.tan
 * @date 2026-10-25 9:08
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = MqConstant.Kafka.CONDITIONAL_ON_PROP)
public class KafkaMessageListenerAspect {
    @Autowired
    private MessageListenerPostProcessorChain messageListenerPostProcessorChain;

    @Pointcut("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public void annotationPointcut4KafkaMessageListener() { }

    /**
     * 处理MDC信息和消息体
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("annotationPointcut4KafkaMessageListener()")
    public Object handleMdcKafkaMessageListener(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return messageListenerPostProcessorChain.handle(() -> {
                try {
                    return joinPoint.proceed(joinPoint.getArgs());
                } catch (Throwable e) {
                    log.error("handleMdcKafkaMessageListener fail", e);
                    throw new RuntimeException(e);
                }
            }, joinPoint.getArgs());
        } catch (MessageListenerPostProcessorChain.MessageListenerPostProcessorChainException e) {
            throw new RuntimeException(e);
        }
    }
}