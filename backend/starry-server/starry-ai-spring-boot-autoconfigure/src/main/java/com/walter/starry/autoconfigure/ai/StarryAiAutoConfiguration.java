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
}
