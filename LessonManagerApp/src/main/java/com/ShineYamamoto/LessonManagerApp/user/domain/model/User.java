package com.ShineYamamoto.LessonManagerApp.user.domain.model;

import java.time.LocalDateTime;

import com.ShineYamamoto.LessonManagerApp.goallevel.domain.model.GoalLevel;

import lombok.Data;

@Data
public class User {
	
	private Long userId;
	
	private String e164PhoneNumber;
	
	private String userName;
	
	private String password;
	
	private Integer goalLevelId;
	
	private String role;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
	
	private GoalLevel goalLevel;
}
