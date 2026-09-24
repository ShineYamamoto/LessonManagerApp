package com.ShineYamamoto.LessonManagerApp.user.form;

import java.time.LocalDateTime;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.GoalLevel;

import lombok.Data;

@Data
public class UserDetailForm {
	
	private Long userId;
	private String e164PhoneNumber;
	private String password;
	private String userName;
	private Integer goalLevelId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private GoalLevel goalLevel;
}
