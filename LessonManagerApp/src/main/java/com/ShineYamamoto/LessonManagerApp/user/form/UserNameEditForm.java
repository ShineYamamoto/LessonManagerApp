package com.ShineYamamoto.LessonManagerApp.user.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UserNameEditForm {

	@NotBlank(message = "ユーザー名を入力してください。")
	@Size(max = 50, message = "ユーザー名は50文字以内で入力してください")
	private String userName;
}
