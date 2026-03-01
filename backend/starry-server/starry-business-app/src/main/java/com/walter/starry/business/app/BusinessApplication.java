package com.walter.starry.business.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

/**
 * @author walter.tan
 */
@SpringBootApplication
@MapperScan("com.walter.starry.business.app.mapper")
@EnableRedisIndexedHttpSession(redisNamespace = "${app.security.namespace-prefix}:" + RedisIndexedSessionRepository.DEFAULT_NAMESPACE)
public class BusinessApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusinessApplication.class, args);
	}

}
