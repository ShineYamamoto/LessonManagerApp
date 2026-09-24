package com.ShineYamamoto.LessonManagerApp.admin.user.form;

import java.time.LocalDateTime;

import com.ShineYamamoto.LessonManagerApp.goallevel.domain.model.GoalLevel;

import lombok.Data;

@Data
public class AdminUserDetailForm {
	
	private Long userId;
	private String e164PhoneNumber;
	private String password;
	private String userName;
	private Integer goalLevelId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private GoalLevel goallevel;
}
