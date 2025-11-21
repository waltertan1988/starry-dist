package com.walter.starry.autoconfigure.mdc.ai.mcp.client;

import java.lang.annotation.*;

/**
 * Mcp Client处理Server事件时支持MDC
 * @author walter.tan
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpServerEventMdcHandler {
}
