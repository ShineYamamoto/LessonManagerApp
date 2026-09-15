package com.ShineYamamoto.LessonManagerApp.login.controller;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;

@Controller
public class LoginController {
	
	private final CountryCodeService countryCodeService;
	
	@Autowired
	public LoginController (CountryCodeService countryCodeService) {
		this.countryCodeService = countryCodeService;
	}

	/** ログイン画面を表示 */
	@GetMapping("/login")
	public String getLogin(Model model, Locale locale) {
		// 国情報をmodelに格納	
		model.addAttribute("countryList", countryCodeService.getCountryList(locale));
		
		return "login/login";
	}
	
	/** ユーザー一覧画面にリダイレクト */
	@PostMapping("/login")
	public String postLogin() {
		return "redirect:/admin/user/list";
	}
}
