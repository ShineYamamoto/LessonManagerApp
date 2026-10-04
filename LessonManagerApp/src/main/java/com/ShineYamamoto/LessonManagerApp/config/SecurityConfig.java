package com.ShineYamamoto.LessonManagerApp.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** セキュリティ対象を設定 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		
		// URL設定
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
				.requestMatchers("/login").permitAll()
				.requestMatchers("/user/signup").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.anyRequest().authenticated()
			).formLogin(login -> login
				.loginPage("/login")
				.usernameParameter("phoneNumber")
				.passwordParameter("password")
				.defaultSuccessUrl("/hello")
				.failureUrl("/login?error")
				.permitAll()
			);
		
		// CSRFを無効（一次無効）
		http.csrf(csrf -> csrf.disable());
		// ヘッダー設定
		http.headers(headers -> headers.frameOptions(option -> option.disable()));
		
		return http.build();
	}
	
	// @Bean
	UserDetailsService userDetailsService() {
		// 一般ユーザー
		UserDetails user = User.withDefaultPasswordEncoder()
			.username("user")
			.password("pass")
			.roles("GENERAL")
			.build();
		UserDetails admin = User.withDefaultPasswordEncoder()
				.username("admin")
				.password("pass")
				.roles("GENERAL", "ADMIN")
				.build();
		return new InMemoryUserDetailsManager(user, admin);
	}
}
