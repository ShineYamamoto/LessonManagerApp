package com.ShineYamamoto.LessonManagerApp.user.form;

import lombok.Data;

@Data
public class SignupForm {
	private String countryCode;
	private String phoneNumber;
	private int userName;
	private String password;
	private Integer goal;
}
