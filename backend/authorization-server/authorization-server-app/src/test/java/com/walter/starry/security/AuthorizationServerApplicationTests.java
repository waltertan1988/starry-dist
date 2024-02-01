package com.walter.starry.security;

import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import com.walter.starry.security.base.bo.AclUserBo;
import com.walter.starry.security.base.component.security.JpaUserDetailsService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author: walter.tan
 * @datetime: 2023/8/27 15:47
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
class AuthorizationServerApplicationTests {

	@Test
	@Disabled
	void contextLoads() {
	}

	@Nested
	class UserDetailsServiceTest {
		@Resource(name = "userDetailsService")
		private JpaUserDetailsService jpaUserDetailsService;

		@Test
		void createUser() {
			Long now = System.currentTimeMillis();

			// 创建普通登录用户
			UserDetails userDetails = User.builder()
					.passwordEncoder(pwd -> "{bcrypt}" + new BCryptPasswordEncoder().encode(pwd))
					.username("user")
					.password("password")
					.accountExpired(false)
					.accountLocked(false)
					.credentialsExpired(false)
					.disabled(false)
					.roles("USER")
					.build();

			String nickname = "普通用户" + RandomStringUtils.randomAlphanumeric(6);
			AclUserBo aclUserBo = new AclUserBo(userDetails.getUsername(), nickname, userDetails.getPassword(), null, null,
				userDetails.isAccountNonExpired(), userDetails.isAccountNonLocked(), userDetails.isCredentialsNonExpired(), userDetails.isEnabled(),
				now, now, now, new HashSet<>(userDetails.getAuthorities())
			);
			jpaUserDetailsService.createUser(aclUserBo);

			// 创建系统管理员
			UserDetails adminUserDetails = User.builder()
					.passwordEncoder(pwd -> "{bcrypt}" + new BCryptPasswordEncoder().encode(pwd))
					.username("admin")
					.password("admin")
					.accountExpired(false)
					.accountLocked(false)
					.credentialsExpired(false)
					.disabled(false)
					.roles("ADMIN")
					.build();

			String adminNickname = "系统管理员" + RandomStringUtils.randomAlphanumeric(6);
			AclUserBo aclAdminUser = new AclUserBo(adminUserDetails.getUsername(), adminNickname, adminUserDetails.getPassword(), null, null,
				adminUserDetails.isAccountNonExpired(), adminUserDetails.isAccountNonLocked(), adminUserDetails.isCredentialsNonExpired(), adminUserDetails.isEnabled(),
				now, now, now, new HashSet<>(adminUserDetails.getAuthorities())
			);
			jpaUserDetailsService.createUser(aclAdminUser);
		}
	}

	@Nested
	class RegisteredClientRepositoryTest {
		@Resource
		private RegisteredClientRepository registeredClientRepository;

		@Test
		void save() {
			RegisteredClient oidcClient = RegisteredClient
					.withId(UUID.randomUUID().toString())
					.clientId("oidc-client")
					.clientName("oidc-client-name")
					// 格式为："{密钥加密器}密钥值"，其中{noop}表示使用NoOpPasswordEncoder进行密钥加密
					.clientSecret("{noop}secret")
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
					.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
					.redirectUri("http://127.0.0.1:8080/login/oauth2/code/oidc-client")
					.postLogoutRedirectUri("http://127.0.0.1:8080/")
					.scope(OidcScopes.OPENID)
					.scope(OidcScopes.PROFILE)
					.scope(OidcScopes.EMAIL)
					.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
					.build();
			registeredClientRepository.save(oidcClient);
		}
	}
}
