package com.walter.starry.authorization.server.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

/**
 * @author walter.tan
 */
@SpringBootApplication
@EnableRedisIndexedHttpSession(redisNamespace = "${app.security.namespace-prefix}:" + RedisIndexedSessionRepository.DEFAULT_NAMESPACE)
public class AuthorizationServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationServerApplication.class, args);
	}

}
