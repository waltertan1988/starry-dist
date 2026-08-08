package com.walter.starry.autoconfigure.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author: walter.tan
 * @DateTime: 2025-07-26 23:19:02
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {"com.walter.starry.autoconfigure.ai"})
public class StarryAiAutoConfiguration {
//    /**
//     * SpringAi-1.0.0原生的ToolCallbackResolver存在问题：如果MCP服务不可用，MCP客户端启动时将会失败。因此重新定义此Bean来解决。参考：
//     * {@link org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration#toolCallbackResolver(org.springframework.context.support.GenericApplicationContext, java.util.List, java.util.List)}
//     * @param mcpClientCommonProperties
//     * @param applicationContext
//     * @param toolCallbacks
//     * @param tcbProviders
//     * @return
//     */
//    @Bean
//    ToolCallbackResolver toolCallbackResolver(McpClientCommonProperties mcpClientCommonProperties, GenericApplicationContext applicationContext,
//                                              List<ToolCallback> toolCallbacks, List<ToolCallbackProvider> tcbProviders) {
//        List<ToolCallback> allFunctionAndToolCallbacks = new ArrayList<>(toolCallbacks);
//        tcbProviders.stream().map(pr -> {
//            if(mcpClientCommonProperties.isInitialized()){
//                return List.of(pr.getToolCallbacks());
//            }else if(pr instanceof SyncMcpToolCallbackProvider || pr instanceof AsyncMcpToolCallbackProvider){
//                return new ArrayList<ToolCallback>();
//            }else{
//                return List.of(pr.getToolCallbacks());
//            }
//        }).filter(CollectionUtils::isNotEmpty).forEach(allFunctionAndToolCallbacks::addAll);
//
//        var staticToolCallbackResolver = new StaticToolCallbackResolver(allFunctionAndToolCallbacks);
//
//        var springBeanToolCallbackResolver = SpringBeanToolCallbackResolver.builder()
//                .applicationContext(applicationContext)
//                .build();
//
//        return new DelegatingToolCallbackResolver(List.of(staticToolCallbackResolver, springBeanToolCallbackResolver));
//    }
}
