package com.walter.starry.autoconfigure.ai.core.tool;

import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.lang.NonNull;

import java.util.Collection;

/**
 * 自定义的异步MCP工具回调提供者
 * @author walter.tan
 */
public class ExtAsyncMcpToolCallbackProvider implements ToolCallbackProvider {

    private final McpAsyncClient mcpClient;

    private final McpToolFilter toolFilter;

    public ExtAsyncMcpToolCallbackProvider(McpAsyncClient mcpClient) {
        this(mcpClient, (c, t) -> true);
    }

    public ExtAsyncMcpToolCallbackProvider(McpAsyncClient mcpClient, Collection<String> toolNames){
        this(mcpClient, (c, t) -> toolNames.contains(t.name()));
    }

    public ExtAsyncMcpToolCallbackProvider(McpAsyncClient mcpClient, McpToolFilter toolFilter) {
        this.mcpClient = mcpClient;
        this.toolFilter = toolFilter;
    }

    @NonNull
    @Override
    public ToolCallback[] getToolCallbacks() {
        if(!mcpClient.isInitialized()){
            synchronized (ExtAsyncMcpToolCallbackProvider.class){
                if(!mcpClient.isInitialized()){
                    mcpClient.initialize().block();
                }
            }
        }

        return AsyncMcpToolCallbackProvider.builder().toolFilter(toolFilter).mcpClients(mcpClient).build().getToolCallbacks();
    }
}
