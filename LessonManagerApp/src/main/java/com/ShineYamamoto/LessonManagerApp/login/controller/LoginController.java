package com.ShineYamamoto.LessonManagerApp.login.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
	public String getLogin(Model model) {
		// 国情報をmodelに格納	
		model.addAttribute("countryList", countryCodeService.getCountryList());
		
		return "login/login";
	}
}
