package com.ShineYamamoto.LessonManagerApp.user.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ShineYamamoto.LessonManagerApp.common.service.PhoneNumberService;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

	private UserMapper mapper;
	private PhoneNumberService phoneNumberService;
	private UserService userService;
	
	// テスト対象の生成は@BeforeEachにまとめる
	@BeforeEach
	void setUp() {
		mapper = mock(UserMapper.class);
		phoneNumberService = mock(PhoneNumberService.class);
		
		userService = new UserService(mapper, phoneNumberService);
	}
	
	
	@Test
	void signupで電話番号をE164形式に変換して登録する() {
		
		// 準備
		User user = new User();
		
		String regionCode = "JP";
		String rawPhoneNumber = "09012345678";
		String expectedE164PhoneNumber = "+819012345678";
		
		when(mapper.insertOne(user)).thenReturn(1);
		when(phoneNumberService.toE164(
				regionCode, 
				rawPhoneNumber
		)).thenReturn(expectedE164PhoneNumber);
		
		// 実行
		userService.signup(user, regionCode, rawPhoneNumber);
		
		// 確認
		assertEquals("ROLE_GENERAL", user.getRole());
		assertEquals("+819012345678", user.getE164PhoneNumber());
		
		//signup()を実行したとき、toE164メソッドが1回呼び出されたことを確認
		verify(phoneNumberService).toE164(
				regionCode,
				rawPhoneNumber
		);
		// signup()を実行したとき、Mapperの登録処理が1回呼び出されたことを確認
		verify(mapper).insertOne(user);
	}
}