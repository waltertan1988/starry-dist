package com.walter.starry.security.base.config;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walter.starry.security.base.bo.MergedOidcUser;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import java.util.Collection;

/**
 * Spring Session配置
 *
 * @author: walter.tan
 * @datetime: 2023/9/9 17:05
 */
@EnableRedisIndexedHttpSession(redisNamespace = "${app.security.namespace-prefix}:" + RedisIndexedSessionRepository.DEFAULT_NAMESPACE)
@Configuration(proxyBeanMethods = false)
public class SessionConfig implements BeanClassLoaderAware {

    private ClassLoader loader;

    /**
     * Session数据使用Json方式进行序列化，参见：https://docs.spring.io/spring-session/reference/configuration/redis.html#serializing-session-using-json
     * @return
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    /**
     * Customized {@link ObjectMapper} to add mix-in for class that doesn't have default
     * constructors
     * @return the {@link ObjectMapper} to use
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));

        // https://github.com/spring-projects/spring-session/issues/2305的临时解决方案(将在Spring Session 3.2.0版本解决)
        mapper.addMixIn(Long.class, LongMixin.class);
        // 参考org.springframework.security.oauth2.client.jackson2.DefaultOidcUserMixin和org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module.setupModule
        mapper.addMixIn(MergedOidcUser.class, MergedOidcUserMixin.class);
        return mapper;
    }

    /*
     * @see
     * org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang
     * .ClassLoader)
     */
    @Override
    public void setBeanClassLoader(@Nonnull ClassLoader classLoader) {
        this.loader = classLoader;
    }

    @Bean
    public SessionRegistry sessionRegistry(FindByIndexNameSessionRepository<? extends Session> findByIndexNameSessionRepository){
        // 需要注册一个自定义的SessionRegistry，否则会在org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer.initSessionRegistry使用默认的SessionRegistry
        return new SpringSessionBackedSessionRegistry<>(findByIndexNameSessionRepository);
    }

    abstract static class LongMixin {
        @JsonProperty("long")
        private Long value;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(value = { "attributes" }, ignoreUnknown = true)
    abstract static class MergedOidcUserMixin {

        @JsonCreator
        MergedOidcUserMixin(@JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities,
                            @JsonProperty("idToken") OidcIdToken idToken, @JsonProperty("userInfo") OidcUserInfo userInfo,
                            @JsonProperty("nameAttributeKey") String nameAttributeKey,
                            @JsonProperty("userDetails") UserDetails userDetails) {
        }

    }
}
