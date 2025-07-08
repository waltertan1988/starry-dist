package com.walter.starry.ai.mcp.server;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * 参考：https://github.com/spring-projects/spring-ai-examples/tree/main/model-context-protocol/weather/starter-webmvc-server
 */
@SpringBootTest(classes = McpServerApplication.class)
public class McpServerTest {
    @Value("${app.version}")
    private String appVersion;

    @Test
    void stdioTest(){
        String currDir = new File(".").getAbsolutePath();
        String jarPath = String.format("%s/target/starry-mcp-server-app-%s.jar", currDir, appVersion);

        var stdioParams = ServerParameters.builder("java")
                .args("-Dspring.ai.mcp.server.stdio=true",
                        "-Dspring.main.web-application-type=none",
                        "-Dlogging.pattern.console=",
                        "-jar", jarPath)
                .build();

        var transport = new StdioClientTransport(stdioParams);

        new SampleClient(transport).run();
    }

    @Test
    void sseTest(){
        var transport = HttpClientSseClientTransport
                .builder("http://localhost:8090")
                .customizeRequest(c -> c.header("starryTraceId", UUID.randomUUID().toString().replace("-", "")))
                .build();
        new SampleClient(transport).run();
    }

    public static class SampleClient {
        private final McpClientTransport transport;

        public SampleClient(McpClientTransport transport) {
            this.transport = transport;
        }

        public void run() {
            var client = McpClient.sync(this.transport).build();

            client.initialize();

            client.ping();

            // List and demonstrate tools
            McpSchema.ListToolsResult toolsList = client.listTools();
            System.out.println("Available Tools = " + toolsList);
            toolsList.tools().forEach(tool -> {
                System.out.println("Tool: " + tool.name() + ", description: " + tool.description() + ", schema: " + tool.inputSchema());
            });

            McpSchema.CallToolResult weatherForcastResult = client.callTool(new McpSchema.CallToolRequest("getWeatherForecastByLocation",
                    Map.of("latitude", "47.6062", "longitude", "-122.3321")));
            System.out.println("Weather Forcast: " + weatherForcastResult);

            McpSchema.CallToolResult alertResult = client.callTool(new McpSchema.CallToolRequest("getAlerts", Map.of("state", "NY")));
            System.out.println("Alert Response = " + alertResult);

            client.closeGracefully();
        }
    }
}
