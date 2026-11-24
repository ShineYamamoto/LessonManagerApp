package com.ShineYamamoto.LessonManagerApp.user.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserDetailDto {
	
	private Long userId;
	private String e164PhoneNumber;
	private String password;
	private String userName;
	private Integer goalLevelId;
	private String displayTimeZone;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
