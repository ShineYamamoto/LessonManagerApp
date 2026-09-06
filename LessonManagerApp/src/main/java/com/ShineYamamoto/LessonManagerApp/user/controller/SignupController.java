package com.ShineYamamoto.LessonManagerApp.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;

@Controller
@RequestMapping("/user")
public class SignupController {
	
	private final CountryCodeService countryCodeService;
	
	/** コンストラクタ */
	@Autowired
	public SignupController (CountryCodeService countryCodeService) {
		this.countryCodeService = countryCodeService;
	}
	
	/** ユーザー登録画面を表示 */
	@GetMapping("/signup")
	public String getSignup(Model model) {
		// 国情報をmodelに格納	
		model.addAttribute("countryList", countryCodeService.getCountryList());
		
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
