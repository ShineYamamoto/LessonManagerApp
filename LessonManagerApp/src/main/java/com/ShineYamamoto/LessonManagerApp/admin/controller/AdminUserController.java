package com.ShineYamamoto.LessonManagerApp.admin.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.admin.user.form.AdminUserDetailForm;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("admin/user")
@RequiredArgsConstructor
public class AdminUserController {
	
	private final UserService userService;
	
	private final ModelMapper modelMapper;

	/** ユーザー一覧画面を表示 */
	@GetMapping("/list")
	public String getUserList(Model model) {
		
		// ユーザー一覧取得
		List<User> userList = userService.getUsers();
		
		// Modelに登録
		model.addAttribute("userList", userList);
		
		// ユーザー一覧画面を表示
		return "admin/user/list";
	}
	
	/** ユーザー詳細画面を表示 */
	@GetMapping("/detail/{userId}")
	public String getUserDetail(
			AdminUserDetailForm form,
			Model model,
			@PathVariable("userId") Long userId) {
		
		// ユーザーを1件取得
		User user = userService.getUserById(userId);
		
		// Userをformに変換
		form = modelMapper.map(user, AdminUserDetailForm.class);
		
		// Modelに登録
		model.addAttribute("userDetailForm", form);
		
		// ユーザー詳細画面を表示
		return "admin/user/detail";
	}
}
