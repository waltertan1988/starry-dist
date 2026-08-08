package com.walter.starry.ai.mcp.server.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author: walter.tan
 * @datetime: 2026/6/13 10:13
 */
@Configuration
//@EnableJpaAuditing
@EntityScan("com.walter.starry.ai.mcp.server.entity")
@EnableJpaRepositories("com.walter.starry.ai.mcp.server.repository")
public class JpaConfig {

}
