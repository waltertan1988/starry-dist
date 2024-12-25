package com.walter.starry.security.base.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author walter.tan
 */
@Configuration
@MapperScan("com.walter.starry.security.base.mapper")
public class MyBatisConfig {

}
