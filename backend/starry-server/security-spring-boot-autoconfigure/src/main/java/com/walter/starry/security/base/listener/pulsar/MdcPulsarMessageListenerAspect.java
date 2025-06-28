package com.walter.starry.security.base.listener.pulsar;

import com.walter.starry.security.base.component.pulsar.PulsarTopicConfig;
import com.walter.starry.security.base.util.MdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

/**
 * Pulsar消费者的MDC切面类
 * @author walter.tan
 * @date 2023-06-16 9:08
 */
@Slf4j
@Aspect
@Component
@ConditionalOnBean(PulsarTopicConfig.class)
public class MdcPulsarMessageListenerAspect {

    @Pointcut("@annotation(org.springframework.pulsar.annotation.PulsarListener)")
    public void annotationPointcut4MdcPulsarMessageListener() { }

    /**
     * 处理MDC信息和消息体
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("annotationPointcut4MdcPulsarMessageListener()")
    public Object handleMdcPulsarMessageListener(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Stream.of(joinPoint.getArgs()).filter(a -> a instanceof Message<?>).findFirst().ifPresent(message -> {
                String traceId = ((Message<?>)message).getProperty(MdcUtil.ATTR_TRACE_ID);
                if(StringUtils.hasText(traceId)){
                    MdcUtil.setTraceId(traceId);
                }
            });
            return joinPoint.proceed(joinPoint.getArgs());
        } catch (Throwable ex) {
            log.error("handleMdcPulsarMessageListener fail", ex);
            throw ex;
        } finally {
            MdcUtil.removeTraceId();
        }
    }
}