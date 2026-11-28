package com.ShineYamamoto.LessonManagerApp.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.form.UserPasswordEditForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/edit/password")
@RequiredArgsConstructor
public class UserPasswordEditController {

	private final UserService userService;
	
	/** パスワード変種画面を表示 */
	@GetMapping
	public String getEdit(
			@ModelAttribute("userPasswordEditForm") UserPasswordEditForm form) {
		
		return "user/edit/password";
	}
}
