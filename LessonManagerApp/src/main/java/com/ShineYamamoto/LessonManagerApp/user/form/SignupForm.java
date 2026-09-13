package com.ShineYamamoto.LessonManagerApp.user.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

import com.ShineYamamoto.LessonManagerApp.user.validation.GoalCheck;

import lombok.Data;

@Data
public class SignupForm {
	
	@NotBlank
	private String countryCode;
	
	@NotBlank
	private String phoneNumber;
	
	@NotBlank
	private String userName;
	
	@NotBlank
	@Length(min = 4, max = 100)
	@Pattern(regexp = "^[\\x21-\\x7E]+$")
	private String password;
	
	@NotNull
	@GoalCheck
	private Integer goal;
}
