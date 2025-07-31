package com.walter.starry.ai.mcp.server.config;

import com.walter.starry.ai.mcp.server.service.StarryMcpService;
import com.walter.starry.ai.mcp.server.service.WeatherService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-30 11:48:50
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider starryTools(StarryMcpService starryMcpService) {
        return MethodToolCallbackProvider.builder().toolObjects(starryMcpService).build();
    }

    @Bean
    public ToolCallbackProvider weatherTools(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }

    @Bean
    public ToolCallback toUpperCase() {
        return FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
                .inputType(TextInput.class)
                .description("Put the text to upper case")
                .build();
    }

    public record TextInput(String input) {}
}
