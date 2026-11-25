package com.ShineYamamoto.LessonManagerApp.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.security.LoginUser;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.form.UserNameEditForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/edit/name")
@RequiredArgsConstructor
public class UserNameEditController {

	private final UserService userService;
	
	@GetMapping
	public String edit(@AuthenticationPrincipal LoginUser loginUser, Model model) {
		User user = userService.getUserById(loginUser.getUserId());
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが存在しません。");
		}
		UserNameEditForm form = new UserNameEditForm();
		form.setUserName(user.getUserName());
		model.addAttribute("userNameEditForm", form);
		return "user/edit/name";
	}
	
	@PostMapping
	public String update(
			@Validated @ModelAttribute("userNameEditForm") UserNameEditForm form,
			BindingResult bindingResult,
			@AuthenticationPrincipal LoginUser loginUser,
			HttpServletRequest request,
			HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		
		if (bindingResult.hasErrors()) {
			return "user/edit/name";
		}
		
		String updatedName = userService.updateUserName(loginUser.getUserId(), form.getUserName());
		redirectAttributes.addFlashAttribute("successMessage", "ユーザー名を変更しました。");
		return "redirect:/user/detail";
	}

}
