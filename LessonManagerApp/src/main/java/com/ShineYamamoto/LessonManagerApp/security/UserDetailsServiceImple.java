package com.ShineYamamoto.LessonManagerApp.security;

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
		
		if (loginUser ==null) {
			throw new UsernameNotFoundException(
				"user not found"
			);
		}
		
		return org.springframework.security.core.userdetails.User
				//.withUsername(loginUser.getE164PhoneNumber())
				.withUsername("+817000000000")
				.password(loginUser.getPassword())
				.authorities(loginUser.getRole())
				.build();
	}
}
