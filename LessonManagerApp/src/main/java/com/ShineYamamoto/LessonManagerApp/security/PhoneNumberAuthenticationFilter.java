package com.ShineYamamoto.LessonManagerApp.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ShineYamamoto.LessonManagerApp.common.service.PhoneNumberService;

/* UsernamePasswordAuthenticationFilterはusername,passwordの情報を取得する前提なので、
 * 新たにPhoneNumberAuthenticationFilterで継承して上書きする
 * ここで作成されたe164PhoneNumber,passwordがspring securityで扱われる
 */
public class PhoneNumberAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

	private final PhoneNumberService phoneNumberService;
	
	public PhoneNumberAuthenticationFilter(
			AuthenticationManager authenticationManager,
			PhoneNumberService phoneNumberService) {
		
		this.phoneNumberService = phoneNumberService;
		
		setAuthenticationManager(authenticationManager);
	}
	
	@Override
	public Authentication attemptAuthentication(
			HttpServletRequest request,
			jakarta.servlet.http.HttpServletResponse response)
			throws AuthenticationException {
		
		// HTMLから値を取得
		String regionCode = request.getParameter("regionCode");
		String phoneNumber = request.getParameter("phoneNumber");
		String password = request.getParameter("password");
		
		// 電話番号をE.164へ変換
		String e164PhoneNumber;
		
		try {
			
			e164PhoneNumber = phoneNumberService.toE164(
					regionCode, phoneNumber
			);
			
		} catch (IllegalArgumentException e) {
			// 電話番号の入力エラーを、Spring Securityの認証失敗として扱う
			throw new BadCredentialsException(
			"電話番号が正しくありません。国・地域と電話番号を確認してください。",
			e);
		}

		
		// 承認情報を作成
		// Spring Security内部で認証情報を持ち回るためのオブジェクト
		// 承認済みか未承認かの情報も持ってる
		UsernamePasswordAuthenticationToken authRequest = UsernamePasswordAuthenticationToken
				.unauthenticated(e164PhoneNumber, password);
		
		// Spring SecurityへE164電話番号、パスワードを渡して承認依頼
		return this.getAuthenticationManager().authenticate(authRequest);
	}
	
}
