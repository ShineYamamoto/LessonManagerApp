package com.ShineYamamoto.LessonManagerApp.user.controller;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.form.UserPhoneEditForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/edit/phone")
@RequiredArgsConstructor
public class UserPhoneEditController {

	private final UserService userService;
	private final CountryCodeService countryCodeService;
	
	/** 電話番号編集画面を表示 */
	@GetMapping
	public String getEdit(
			@AuthenticationPrincipal LoginUser loginUser,
			@ModelAttribute("userPhoneEditForm") UserPhoneEditForm form,
			Model model,
			Locale locale) {
		
		return showEditPage(loginUser, model, locale);
	}
	
	/** 編集画面に必要な情報をModelに登録する */
	private String showEditPage(
			LoginUser loginUser,
			Model model,
			Locale locale) {
		
		User user = userService.getUserById(loginUser.getUserId());
		
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		
		// 現在の電話番号
		model.addAttribute(
				"currentPhoneNumber",
				user.getE164PhoneNumber());
		
		model.addAttribute(
				"countryList",
				countryCodeService.getCountryList(locale));
		
		return "user/edit/phone";
	}
	
}
