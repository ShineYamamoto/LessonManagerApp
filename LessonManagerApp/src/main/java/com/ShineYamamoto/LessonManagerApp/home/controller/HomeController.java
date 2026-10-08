package com.ShineYamamoto.LessonManagerApp.home.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	/** ホーム画面を表示 */
	@GetMapping("/home")
	public String getHome() {
		// ホーム画面を作成するまでは既存の画面を借り表示する
		return "home/home";
	}
}
