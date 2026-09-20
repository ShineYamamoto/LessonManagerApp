package com.ShineYamamoto.LessonManagerApp.user.controller;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.form.UserDetailForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDetailController {

	private final UserService userService;
	
	private final ModelMapper modelMapper;
	
	/** ユーザー詳細画面を表示 */
	@GetMapping("/detail/{userId}")
	public String getUserDetail(UserDetailForm form, Model model, @PathVariable("userId") Long userId) {
		
		// ユーザー1件取得
		User user = userService.getUserById(userId);
		
		// Userをformに変換
		form = modelMapper.map(user, UserDetailForm.class);
		
		// Modelに登録
		model.addAttribute("userDetailForm", form);
		
		// ユーザー詳細画面を表示
		return "user/detail";
	}
}
