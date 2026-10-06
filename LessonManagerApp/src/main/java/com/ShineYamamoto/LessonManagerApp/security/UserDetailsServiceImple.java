package com.ShineYamamoto.LessonManagerApp.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImple implements UserDetailsService {
	
	private final UserMapper userMapper;
	
	@Override
	public UserDetails loadUserByUsername(String e164PhoneNumber) throws UsernameNotFoundException {
		
		User loginUser = userMapper.findByE164PhoneNumber(e164PhoneNumber);
		
		if (loginUser == null) {
			throw new UsernameNotFoundException(
				"user not found"
			);
		}
		
		// ロールList作成
		GrantedAuthority authority = new SimpleGrantedAuthority(loginUser.getRole());
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(authority);
		
		// UserDetails生成
		UserDetails userDetails = new LoginUser(
				loginUser.getE164PhoneNumber(),
				loginUser.getPassword(),
				authorities,
				loginUser.getUserName());
		
		return userDetails;
	}
}
