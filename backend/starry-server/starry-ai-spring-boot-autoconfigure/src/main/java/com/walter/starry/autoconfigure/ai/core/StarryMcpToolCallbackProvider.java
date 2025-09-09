package com.walter.starry.autoconfigure.ai.core;

import com.google.common.collect.Lists;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * @author walter.tan
 */
@Slf4j
public class StarryMcpToolCallbackProvider implements ToolCallbackProvider {

    private final List<?> mcpClients;

    private final BiPredicate<McpSyncClient, McpSchema.Tool> syncToolFilter;
    private final BiPredicate<McpAsyncClient, McpSchema.Tool> asyncToolFilter;

    public StarryMcpToolCallbackProvider(List<?> mcpClients) {
        this(mcpClients, (c, t) -> true);
    }

    public StarryMcpToolCallbackProvider(List<?> mcpClients, Collection<String> toolNames){
        this(mcpClients, (c, t) -> toolNames.contains(t.name()));
    }

    public StarryMcpToolCallbackProvider(List<?> mcpClients, BiPredicate<Object, McpSchema.Tool> toolFilter) {
        this.mcpClients = mcpClients;
        syncToolFilter = mcpClients.getFirst() instanceof McpSyncClient ? toolFilter::test : null;
        asyncToolFilter = mcpClients.getFirst() instanceof McpAsyncClient ? toolFilter::test : null;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        List<McpSyncClient> syncClients = Lists.newArrayList();
        List<McpAsyncClient> asyncClients = Lists.newArrayList();
        List<ToolCallback> callbackList = Lists.newArrayList();

        for (Object mcpClient : mcpClients) {
            if(mcpClient instanceof McpSyncClient client){
                if(!client.isInitialized()){
                    synchronized (StarryMcpToolCallbackProvider.class){
                        if(!client.isInitialized()){
                            try{
                                client.initialize();
                            }catch (Throwable t){
                                log.error("Initialize McpSyncClient failed. name: {}, version: {}", client.getClientInfo().name(), client.getClientInfo().version(), t);
                            }
                        }
                    }
                }
                if(client.isInitialized()){
                    syncClients.add(client);
                }
            }else if(mcpClient instanceof McpAsyncClient client){
                if(!client.isInitialized()){
                    synchronized (StarryMcpToolCallbackProvider.class){
                        if(!client.isInitialized()){
                            try{
                                client.initialize().block();
                            }catch (Throwable t){
                                log.error("Initialize McpAsyncClient failed. name: {}, version: {}", client.getClientInfo().name(), client.getClientInfo().version(), t);
                            }
                        }
                    }
                }
                if(client.isInitialized()){
                    asyncClients.add(client);
                }
            }else{
                throw new UnsupportedOperationException("Unsupported mcpClient type: " + mcpClient.getClass());
            }
        }

        if(!CollectionUtils.isEmpty(syncClients)){
            callbackList.addAll(Arrays.asList(new SyncMcpToolCallbackProvider(syncToolFilter, syncClients).getToolCallbacks()));
        }

        if(!CollectionUtils.isEmpty(asyncClients)){
            callbackList.addAll(Arrays.asList(new AsyncMcpToolCallbackProvider(asyncToolFilter, asyncClients).getToolCallbacks()));
        }

        return callbackList.toArray(new ToolCallback[0]);
    }
}
