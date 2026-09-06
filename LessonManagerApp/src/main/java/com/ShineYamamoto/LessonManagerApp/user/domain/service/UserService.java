package com.ShineYamamoto.LessonManagerApp.user.domain.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class UserService {
	
	/** 目標レベルのMapを生成する */
	public Map<String, Integer> getGoalMap() {
		Map<String, Integer> goalMap = new LinkedHashMap<>();
		goalMap.put("N1", 1);
		goalMap.put("N2", 2);
		goalMap.put("N3", 3);
		goalMap.put("N4", 4);
		goalMap.put("N5", 5);
		
		return goalMap;
	}
}
