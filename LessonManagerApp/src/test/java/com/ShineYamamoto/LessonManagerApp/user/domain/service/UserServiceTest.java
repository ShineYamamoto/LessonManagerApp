package com.ShineYamamoto.LessonManagerApp.user.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

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
	
	@Test
	void getGoalMapでN1の値は1になる() {
		
		// 準備
		UserService userService = new UserService();
		
		// 実行
		Map<String, Integer> goalMap = userService.getGoalMap();
		
		// 確認
		assertEquals(1, goalMap.get("N1"));
	}
}
