package com.walter.starry.autoconfigure.ai.core.tool;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Collection;
import java.util.function.BiPredicate;

/**
 * @author walter.tan
 */
@Slf4j
public class ExtAsyncMcpToolCallbackProvider implements ToolCallbackProvider {

    private final McpSyncClient mcpClient;

    private final BiPredicate<McpSyncClient, McpSchema.Tool> toolFilter;

    public ExtAsyncMcpToolCallbackProvider(McpSyncClient mcpClient) {
        this(mcpClient, (c, t) -> true);
    }

    public ExtAsyncMcpToolCallbackProvider(McpSyncClient mcpClient, Collection<String> toolNames){
        this(mcpClient, (c, t) -> toolNames.contains(t.name()));
    }

    public ExtAsyncMcpToolCallbackProvider(McpSyncClient mcpClient, BiPredicate<Object, McpSchema.Tool> toolFilter) {
        this.mcpClient = mcpClient;
        this.toolFilter = toolFilter::test;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        if(!mcpClient.isInitialized()){
            synchronized (ExtAsyncMcpToolCallbackProvider.class){
                if(!mcpClient.isInitialized()){
                    mcpClient.initialize();
                }
            }
        }
        
        return new SyncMcpToolCallbackProvider(toolFilter, mcpClient).getToolCallbacks();
    }
}
