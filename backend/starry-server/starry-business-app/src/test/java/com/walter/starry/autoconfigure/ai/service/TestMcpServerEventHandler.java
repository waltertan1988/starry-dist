package com.walter.starry.autoconfigure.ai.service;

import com.walter.starry.autoconfigure.mdc.ai.mcp.client.McpServerEventMdcHandler;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.stereotype.Component;

/**
 * 接收并处理MCP Server发送过来事件
 * @author walter.tan
 */
@Slf4j
@Component
public class TestMcpServerEventHandler {

    /**
     * 处理日志事件
     * @param notification
     */
    @McpLogging(clients = "starry")
    @McpServerEventMdcHandler
    public void handleLogs(McpSchema.LoggingMessageNotification notification) {
        log.info("接收到的Server端Logging信息：level: {}, data: {}", notification.level(), notification.data());
    }

    /**
     * 处理进度事件
     * @param notification
     */
    @McpProgress(clients = "starry")
    @McpServerEventMdcHandler
    public void handleProgress(McpSchema.ProgressNotification notification) {
        double percentage = notification.progress() * 100;
        log.info("接收到的Server端Progress信息：percentage: {}, message: {}", percentage, notification.message());
    }
}
