package com.ShineYamamoto.LessonManagerApp.login.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import com.ShineYamamoto.LessonManagerApp.common.model.CountryCodeModel;
import com.ShineYamamoto.LessonManagerApp.common.service.CountryCodeService;

@ExtendWith(MockitoExtension.class)
public class LoginControllerTest {
	
	@Mock
	private CountryCodeService countryCodeService;
	
	@InjectMocks
	private LoginController loginController;
	
	@Test
	void getLoginで国情報をModelに格納してログイン画面を返す() {
		
		// 準備
		Locale locale = Locale.JAPANESE;
		
		List<CountryCodeModel> countryList = List.of(
			new CountryCodeModel(
					"中国", "+86", "CN", "例:13812345678"),
			new CountryCodeModel(
					"日本", "+81", "JP", "例:09012345678")
		);
		
		when(countryCodeService.getCountryList(locale))
				.thenReturn(countryList);
		
		ExtendedModelMap model = new ExtendedModelMap();
		
		// 実行
		String viewName = loginController.getLogin(model, locale);
		
		// 確認
		assertEquals("login/login", viewName);
		assertSame(countryList, model.get("countryList"));
		
		verify(countryCodeService).getCountryList(locale);
	}
}
