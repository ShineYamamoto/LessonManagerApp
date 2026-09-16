package com.ShineYamamoto.LessonManagerApp.user.domain.model;

import lombok.Data;

@Data
public class User {

	private String regionCode;
	
	private String phoneNumber;
	
	private String userName;
	
	private String password;
	
	private Integer goal;
	
	private String role;
}
