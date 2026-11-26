package com.ShineYamamoto.LessonManagerApp.user.controller;

import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DuplicateKeyException;
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

import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService.PhoneUpdateResult;
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
	
	/** 電話番号更新処理 */
	@PostMapping
	public String postEdit(
			@AuthenticationPrincipal LoginUser loginUser,
			@ModelAttribute("userPhoneEditForm") @Validated UserPhoneEditForm form,
			BindingResult bindingResult,
			Model model,
			Locale locale,
			HttpServletRequest request) throws ServletException {
		
		// 入力チェック
		if (bindingResult.hasErrors()) {
			return showEditPage(loginUser, model, locale);
		}
		
		PhoneUpdateResult result;
		
		try {
			
			result = userService.updatePhoneNumber(
					loginUser.getUserId(),
					loginUser.getUsername(),
					form.getRegionCode(),
					form.getPhoneNumber(),
					form.getCurrentPassword());
		
		} catch (DuplicateKeyException e) {
			
			bindingResult.rejectValue(
					"phoneNumber",
					"user.phone.edit.error.duplicate");
			
			return showEditPage(loginUser, model, locale);
		}
		
		// 更新結果に応じて処理を分岐
		switch (result) {
		
			case INVALID_PHONE ->
				bindingResult.rejectValue(
						"phoneNumber",
						"phoneNumber.invalid");
				
			case INVALID_PASSWORD ->
				bindingResult.rejectValue(
						"currentPassword",
						"user.phone.edit.error.password");
			
			case SAME_PHONE ->
				bindingResult.rejectValue(
						"phoneNumber",
						"user.phone.edit.error.same");
				
			case STALE_LOGIN -> {
				request.logout();
				return "redirect:/login?reauth";
			}
			
			case SUCCESS -> {
				request.logout();
				return "redirect:/login?phoneChanged";
			}
		}
		
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
