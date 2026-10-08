package com.ShineYamamoto.LessonManagerApp.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.ShineYamamoto.LessonManagerApp.common.service.PhoneNumberService;
import com.ShineYamamoto.LessonManagerApp.security.PhoneNumberAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/** セキュリティ対象を設定 */
	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			PhoneNumberAuthenticationFilter phoneNumberAuthenticationFilter)
			throws Exception {
		
		// URL設定
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
				.requestMatchers("/login").permitAll()
				.requestMatchers("/user/signup").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers("/admin/user/list").hasAuthority("ROLE_ADMIN")
				.anyRequest().authenticated()
			).formLogin(form -> form.disable()
			).exceptionHandling(exception -> exception
				.authenticationEntryPoint(
					new LoginUrlAuthenticationEntryPoint("/login")
				)
			).logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/login?logout")
			).rememberMe(remember -> remember
				.rememberMeParameter("remember-me")
				.tokenValiditySeconds(3600)
			);
		
		// 自作したFilterをSpring Securityの認証処理の流れに組み込む
		http
			.addFilterAt(
				phoneNumberAuthenticationFilter,
				UsernamePasswordAuthenticationFilter.class
			);
		
		// Spring Securityの設定を構築する
		SecurityFilterChain filterChain = http.build();
		
		// rememberMe設定によって作成されたサービスを取得する
		RememberMeServices rememberMeServices = http.getSharedObject(RememberMeServices.class);
		
		// 自作フィルターにログイン成功時のCookie発行処理を設定する
		phoneNumberAuthenticationFilter.setRememberMeServices(rememberMeServices);
		
		
		return filterChain;
	}
	
	@Bean
	AuthenticationManager authenticationManager(
			AuthenticationConfiguration configuration)
			throws Exception {
		
		return configuration.getAuthenticationManager();
	}
	
	/* Filterを作って設定する */
	@Bean
	PhoneNumberAuthenticationFilter phoneNumbereAuthenticationFilter(
			AuthenticationManager authenticationManager,
			PhoneNumberService phoneNumberService) {
		
		// 自作したFilterを生成
		PhoneNumberAuthenticationFilter filter =
				new PhoneNumberAuthenticationFilter(
						authenticationManager,
						phoneNumberService
				);
		
		// URLへのリクエストを認証対象にするかを設定
		filter.setRequiresAuthenticationRequestMatcher(
			PathPatternRequestMatcher.withDefaults()
				.matcher(HttpMethod.POST, "/login")

		);
		
		// ログイン成功時の処理
		SavedRequestAwareAuthenticationSuccessHandler successHandler = 
				new SavedRequestAwareAuthenticationSuccessHandler();
		
		// ログイン前のアクセス先が保存されていなければ以下の設定urlへ遷移
		successHandler.setDefaultTargetUrl("/home");
		
		// 保存済みのアクセス先より /home を優先する
		successHandler.setAlwaysUseDefaultTargetUrl(true);
		
		filter.setAuthenticationSuccessHandler(successHandler);
		
		// ログイン失敗時の遷移先
		filter.setAuthenticationFailureHandler(
				new SimpleUrlAuthenticationFailureHandler(
					"/login?error"
				)
		);
		
		// 承認情報をセッションに保存し、次のリクエストでもログイン状態を維持
		filter.setSecurityContextRepository(
				new HttpSessionSecurityContextRepository());
		
		return filter;
	}
}
