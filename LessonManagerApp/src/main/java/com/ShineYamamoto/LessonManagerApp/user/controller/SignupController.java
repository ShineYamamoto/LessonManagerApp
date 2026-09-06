package com.ShineYamamoto.LessonManagerApp.user.controller;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;
import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;

@Controller
@RequestMapping("/user")
public class SignupController {
	
	private final CountryCodeService countryCodeService;
	private final UserService userService;
	
	/** コンストラクタ */
	@Autowired
	public SignupController (CountryCodeService countryCodeService, UserService userService) {
		this.countryCodeService = countryCodeService;
		this.userService = userService;
	}
	
	/** ユーザー登録画面を表示 */
	@GetMapping("/signup")
	public String getSignup(Model model, Locale locale) {
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
	public String postSignup() {
		// ログイン画面にリダイレクト
		return "redirect:/login";
	}
}
