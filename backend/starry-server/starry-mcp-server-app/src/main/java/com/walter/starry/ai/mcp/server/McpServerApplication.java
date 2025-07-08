package com.walter.starry.ai.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.scheduler.Schedulers;

/**
 * @author walter.tan
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);

        // 线程池注册MDC特性
        Schedulers.onScheduleHook("mdcHook", runnable -> {
            return runnable;
        });
    }
}
