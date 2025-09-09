package com.walter.starry.autoconfigure.ai.conf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.SpringBeanToolCallbackResolver;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2025-07-26 23:19:02
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {"com.walter.starry.autoconfigure.ai"})
public class StarryAiAutoConfiguration {
    /**
     * 原生的ToolCallbackResolver存在问题：如果MCP服务不可用，MCP客户端启动时将会失败。因此重新定义此Bean来解决。参考：
     * {@link org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration#toolCallbackResolver(org.springframework.context.support.GenericApplicationContext, java.util.List, java.util.List)}
     * @param applicationContext
     * @param toolCallbacks
     * @param tcbProviders
     * @return
     */
    @Bean
    @ConditionalOnProperty(value = "spring.ai.mcp.client.initialized", havingValue = "false")
    ToolCallbackResolver toolCallbackResolver(GenericApplicationContext applicationContext,
                                              List<ToolCallback> toolCallbacks, List<ToolCallbackProvider> tcbProviders) {
        List<ToolCallback> allFunctionAndToolCallbacks = new ArrayList<>(toolCallbacks);
        tcbProviders.stream().map(pr -> {
            if(pr instanceof SyncMcpToolCallbackProvider || pr instanceof AsyncMcpToolCallbackProvider){
                return new ArrayList<ToolCallback>();
            }else{
                return List.of(pr.getToolCallbacks());
            }
        }).filter(CollectionUtils::isNotEmpty).forEach(allFunctionAndToolCallbacks::addAll);

        var staticToolCallbackResolver = new StaticToolCallbackResolver(allFunctionAndToolCallbacks);

        var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
                .applicationContext(applicationContext)
                .build();

        return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver, springBeanToolCallbackResolver));
    }
}
