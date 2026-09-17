package com.ShineYamamoto.LessonManagerApp.user.controller;

import java.util.Locale;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;
import com.ShineYamamoto.LessonManagerApp.user.form.SignupForm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class SignupController {
	
	private final CountryCodeService countryCodeService;
	private final UserService userService;
	private final ModelMapper modelMapper;
	
	
	/** ユーザー登録画面を表示 */
	@GetMapping("/signup")
	public String getSignup(Model model,@ModelAttribute SignupForm form, Locale locale) {
		// 国情報をmodelに格納	
		model.addAttribute("countryList", countryCodeService.getCountryList(locale));
		
		// 目標レベルをmodelに格納
		Map<String, Integer> goalMap = userService.getGoalMap();
		model.addAttribute("goalMap", goalMap);
		
		// ユーザー登録画面に画面遷移
		return "user/signup";
	}
	
	/** ユーザー登録処理 */
	@PostMapping("/signup")
	public String postSignup(
			Model model,
			@ModelAttribute @Validated SignupForm form,
			BindingResult bindingResult,
			Locale locale) {
		
		// 入力チェック結果
		if (bindingResult.hasErrors()) {
			// NG：ユーザー登録画面に戻る
			return getSignup(model, form, locale);
		}
		
		
		log.info(form.toString());
		
		// formをUserクラスに変換
		User user = modelMapper.map(form, User.class);
		
		try {
			userService.signup(user, form.getRegionCode(), form.getPhoneNumber());
		} catch (IllegalArgumentException e) {
			
			bindingResult.rejectValue(
					"phoneNumber", "phoneNumber.invalid"
			);
			
			return getSignup(model, form, locale);
		}
		
		// ユーザー登録
		//userService.signup(user, form.getRegionCode(), form.getPhoneNumber());
		
		// ログイン画面にリダイレクト
		return "redirect:/login";
	}
}
