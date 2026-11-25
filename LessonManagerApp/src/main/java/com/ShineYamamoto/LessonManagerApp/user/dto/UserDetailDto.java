package com.ShineYamamoto.LessonManagerApp.user.dto;

import lombok.Data;

@Data
public class UserDetailDto {
	
	private String e164PhoneNumber;
	private String userName;
	private Integer goalLevelId;
	private String goalLevelCode;
	private String displayTimeZone;
}
