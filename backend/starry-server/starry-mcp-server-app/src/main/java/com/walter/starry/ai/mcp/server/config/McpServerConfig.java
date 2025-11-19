package com.walter.starry.ai.mcp.server.config;

import org.springframework.context.annotation.Configuration;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-30 11:48:50
 */
@Configuration
public class McpServerConfig {

//    @Bean
//    public ToolCallbackProvider starryTools(StarryMcpService starryMcpService) {
//        return MethodToolCallbackProvider.builder().toolObjects(starryMcpService).build();
//    }
//
//    @Bean
//    public ToolCallbackProvider weatherTools(WeatherService weatherService) {
//        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
//    }
//
//    @Bean
//    public ToolCallback toUpperCase() {
//        return FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
//                .inputType(TextInput.class)
//                .description("Put the text to upper case")
//                .build();
//    }
//
//    public record TextInput(String input) {}

//    @Bean
//    public ToolCallbackProvider starryAiTools(List<AiTool> aiTools) {
//        return MethodToolCallbackProvider.builder().toolObjects(aiTools.toArray(new Object[0])).build();
//    }
}
