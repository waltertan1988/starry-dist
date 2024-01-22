package com.walter.starry.security.base.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * @author walter.tan
 */
@Configuration
//@EnableJpaAuditing
@EntityScan("com.walter.starry.security.base.entity")
@EnableJpaRepositories("com.walter.starry.security.base.repository")
public class JpaConfig {

}
