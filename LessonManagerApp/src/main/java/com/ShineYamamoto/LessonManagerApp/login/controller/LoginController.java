package com.ShineYamamoto.LessonManagerApp.login.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ShineYamamoto.LessonManagerApp.login.application.LoginApplicationService;

@Controller
public class LoginController {
	
	private final LoginApplicationService loginApplicationService;
	
	/** コンストラクタ */
	@Autowired
	public LoginController (LoginApplicationService loginApplicationService) {
		this.loginApplicationService = loginApplicationService;
	}

	/** ログイン画面を表示 */
	@GetMapping("/login")
	public String getLogin(Model model) {
		// 国番号を取得
		Map<String, String> countryCodeMap = loginApplicationService.getCountryCodeMap();
		model.addAttribute("countryCodeMap", countryCodeMap);
		
		return "login/login";
	}
}
