package com.ShineYamamoto.LessonManagerApp.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

@Getter
public class LoginUser extends User {
	
	/** 画面に表示するユーザー名 */
	private final String displayUserName;
	
	public LoginUser(
			String e164PhoneNumber,
			String password,
			Collection<? extends GrantedAuthority> authorities,
			String displayUserName) {
		
		super(e164PhoneNumber, password, authorities);
		this.displayUserName = displayUserName;
	}
}
