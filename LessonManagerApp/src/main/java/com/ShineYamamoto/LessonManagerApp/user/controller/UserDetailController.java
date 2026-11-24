package com.ShineYamamoto.LessonManagerApp.user.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.ShineYamamoto.LessonManagerApp.security.LoginUser;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.dto.UserDetailDto;
import com.ShineYamamoto.LessonManagerApp.user.form.UserDetailForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserDetailController {

	private final UserService userService;
	
	private final ModelMapper modelMapper;
	
	/** ユーザー詳細画面を表示 */
	@GetMapping("/detail")
	public String getUserDetail(
			@AuthenticationPrincipal LoginUser loginUser,
			Model model) {
		
		// ユーザー1件取得
		User user = userService.getUserById(loginUser.getUserId());
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが存在しません");
		}
		
		// Userをformに変換
		UserDetailDto dto = modelMapper.map(user, UserDetailDto.class);
		
		// Modelに登録
		model.addAttribute("userDetailDto", dto);
		
		// ユーザー詳細画面を表示
		return "user/detail";
	}
	
	/** ユーザー更新処理 */
	@PostMapping(value = "/detail", params = "update")
	public String updateUser(
			UserDetailForm form,
			@AuthenticationPrincipal LoginUser loginUser,
			Model model) {
		
		// ユーザーを更新
		userService.updateUserById(
				loginUser.getUserId(),
				form.getPassword(),
				form.getUserName()
		);
		
		// ユーザー詳細画面にリダイレクト
		return "redirect:/user/detail/";
	}
	
	/** ユーザー削除処理し、ログアウトする */
	@PostMapping(value = "/detail", params = "delete")
	public String deleteUser(
			@AuthenticationPrincipal LoginUser loginUser,
			HttpServletRequest request) throws ServletException {
		
		// ユーザーを削除
		userService.deleteUserById(loginUser.getUserId());
		
		// セッション・認証情報・remember-meを終了
		request.logout();
		
		// サインアップ画面にリダイレクト
		return "redirect:/user/signup";
	}
}
