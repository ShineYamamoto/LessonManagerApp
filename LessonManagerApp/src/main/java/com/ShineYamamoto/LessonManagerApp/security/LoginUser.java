package com.ShineYamamoto.LessonManagerApp.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

@Getter
public class LoginUser extends User {
	
	/** DBのユーザーID */
	private final Long userId;
	
	/** 画面に表示するユーザー名 */
	private final String displayUserName;
	
	public LoginUser(
			Long userId,
			String e164PhoneNumber,
			String password,
			Collection<? extends GrantedAuthority> authorities,
			String displayUserName) {
		
		super(e164PhoneNumber, password, authorities);
		this.userId = userId;
		this.displayUserName = displayUserName;
	}
}
