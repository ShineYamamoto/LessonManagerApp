package com.ShineYamamoto.LessonManagerApp.user.controller;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
		//model.addAttribute("reservationList", user.getReservationList());
		
		// ユーザー詳細画面を表示
		return "user/detail";
	}
	
	/** ユーザー更新処理 */
	@PostMapping(value = "/detail", params = "update")
	public String updateUser(UserDetailForm form, Model model) {
		
		// ユーザーを更新
		userService.updateUserById(
				form.getUserId(),
				form.getPassword(),
				form.getUserName()
		);
		
		// ユーザー詳細画面にリダイレクト
		return "redirect:/user/detail/" + form.getUserId();
	}
	
	/** ユーザー削除処理 */
	@PostMapping(value = "/detail", params = "delete")
	public String deleteUser(UserDetailForm form, Model model) {
		
		// ユーザーを削除
		userService.deleteUserById(form.getUserId());
		
		// サインアップ画面にリダイレクト
		return "redirect:/user/signup";
	}
}
