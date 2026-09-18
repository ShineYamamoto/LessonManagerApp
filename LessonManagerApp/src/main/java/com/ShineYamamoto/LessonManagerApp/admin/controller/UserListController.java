package com.ShineYamamoto.LessonManagerApp.admin.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("admin/user")
@RequiredArgsConstructor
public class UserListController {
	
	private final UserService userService;

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
}
