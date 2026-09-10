package com.ShineYamamoto.LessonManagerApp.user.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class UserServiceTest {
	
	@Test
	void getGoalMapで5種類のレベルが取得できる() {
		
		// 準備
		UserService userService = new UserService();
		
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
		
		// 準備
		UserService userService = new UserService();
		
		// 実行
		Map<String, Integer> goalMap = userService.getGoalMap();
		
		// 確認
		assertEquals(expectedValue, goalMap.get(level).intValue());
	}
}
