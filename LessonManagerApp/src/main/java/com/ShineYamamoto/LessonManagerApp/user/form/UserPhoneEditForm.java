package com.ShineYamamoto.LessonManagerApp.user.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPhoneEditForm {

	@NotBlank
	@Pattern(regexp = "^(JP|CN)$")
	private String regionCode;
	
	@NotBlank
	private String phoneNumber;
	
	@NotBlank
	@Length(min = 4, max = 100)
	@Pattern(regexp = "^[\\x21-\\x7E]+$")
	private String currentPassword;
}
