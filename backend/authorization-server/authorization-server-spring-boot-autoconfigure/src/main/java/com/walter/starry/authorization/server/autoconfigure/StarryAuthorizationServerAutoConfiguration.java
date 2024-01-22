package com.walter.starry.authorization.server.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author: walter.tan
 * @DateTime: 2023-12-12 23:19:02
 */
@AutoConfiguration
@ComponentScan(basePackages = {"com.walter.starry.authorization.server.base"})
public class StarryAuthorizationServerAutoConfiguration {
}
