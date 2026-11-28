package com.ShineYamamoto.LessonManagerApp.user.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPasswordEditForm {

	@NotBlank
	@Size(min = 4, max = 100)
	private String currentPassword;
	
	@NotBlank
	@Size(min = 4, max = 100)
	@Pattern(regexp = "^[\\x21-\\x7E]+$")
	private String newPassword;
	
	@NotBlank
	@Size(min = 4, max = 100)
	private String confirmPassword;
}
