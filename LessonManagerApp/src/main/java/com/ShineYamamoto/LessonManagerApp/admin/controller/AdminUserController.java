package com.ShineYamamoto.LessonManagerApp.admin.controller;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.ShineYamamoto.LessonManagerApp.admin.user.form.AdminUserDetailForm;
import com.ShineYamamoto.LessonManagerApp.admin.user.form.AdminUserListForm;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("admin/user")
@RequiredArgsConstructor
@SessionAttributes(types = AdminUserListForm.class)
public class AdminUserController {
	
	/** セッションに登録 */
	@ModelAttribute("adminUserListForm")
	private AdminUserListForm setUserListForm() {
		return new AdminUserListForm();
	}
	
	private final UserService userService;
	
	private final ModelMapper modelMapper;

	/** ユーザー一覧画面を表示 */
	@GetMapping("/list")
	public String getUserList(
				Model model, 
				@ModelAttribute AdminUserListForm form,
				@PageableDefault(page = 0, size = 3) Pageable pageable) {
		
		// formをUserクラスに変換
		User user = modelMapper.map(form, User.class);
		
		// ユーザー一覧取得
		Page<User> userPage = userService.getUsers(user, pageable);
		
		// Modelに登録
		model.addAttribute("userList", userPage.getContent());
		model.addAttribute("page", userPage);
		
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
