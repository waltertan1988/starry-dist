package com.walter.starry.autoconfigure.mdc.ai.mcp.client;

import com.walter.starry.common.util.MdcUtil;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

/**
 * Mcp Client处理Server事件时支持MDC的处理器切面类
 * @author walter.tan
 * @date 2025-11-16 9:08
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass(McpSchema.Meta.class)
public class MdcMcpServerEventHandlerAspect {

    @Pointcut("@annotation(com.walter.starry.autoconfigure.mdc.ai.mcp.client.McpServerEventMdcHandler)")
    public void annotationPointcut4MdcMcpServerEventHandler() { }

    /**
     * 加入MDC
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("annotationPointcut4MdcMcpServerEventHandler()")
    public Object handleMdcMcpServerEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        Arrays.stream(joinPoint.getArgs())
                .filter(args -> Objects.nonNull(args) && args instanceof McpSchema.Meta)
                .map(args -> (McpSchema.Meta) args)
                .map(meta -> meta.meta().get(MdcUtil.ATTR_TRACE_ID))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .findFirst()
                .ifPresent(MdcUtil::setTraceId);
        return joinPoint.proceed(joinPoint.getArgs());
    }
}