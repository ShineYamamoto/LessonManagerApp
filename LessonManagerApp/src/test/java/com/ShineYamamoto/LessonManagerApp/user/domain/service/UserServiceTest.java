package com.ShineYamamoto.LessonManagerApp.user.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

	private UserMapper mapper;
	private UserService userService;
	
	// テスト対象の生成は@BeforeEachにまとめる
	@BeforeEach
	void setUp() {
		mapper = mock(UserMapper.class);
		userService = new UserService(mapper);
	}
	
	@Test
	void getGoalMapで5種類のレベルが取得できる() {
		
		// 実行
		Map<String, Integer> goalMap = userService.getGoalMap();
		
		// 確認
		assertEquals(5, goalMap.size());
	}
	
	@ParameterizedTest
	@CsvSource({
		"N1, 1",
		"N2, 2",
		"N3, 3",
		"N4, 4",
		"N5, 5"
	})
	void getGoalMapで各レベルに正しい値が設定される(
			String level,
			int expectedValue) {
		
		// 実行
		Map<String, Integer> goalMap = userService.getGoalMap();
		
		// 確認
		assertEquals(expectedValue, goalMap.get(level).intValue());
	}
	
	
	
	@Test
	void signupで一般ユーザー権限を設定して登録する() {
		
		// 準備
		User user = new User();
		when(mapper.insertOne(user)).thenReturn(1);
		
		// 実行
		userService.signup(user);
		
		// 確認
		assertEquals("ROLE_GENERAL", user.getRole());
		// signup()を実行したとき、Mapperの登録処理が1回呼び出されたこと」を確認
		verify(mapper).insertOne(user);
	}
}
