package com.ShineYamamoto.LessonManagerApp.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.ShineYamamoto.LessonManagerApp.common.model.CountryCodeModel;

@ExtendWith(MockitoExtension.class)
public class CountryCodeServiceTest {

	@Mock
	private MessageSource messageSource;
	
	@InjectMocks
	private CountryCodeService countryCodeService;
	
	@Test
	void getCountryListで中国と日本の情報を取得できる() {
		
		// 準備
		Locale locale = Locale.JAPANESE;
		
		when(messageSource.getMessage(
				"country.china", null, locale))
				.thenReturn("中国");
		
		when(messageSource.getMessage(
				"country.japan", null, locale))
				.thenReturn("日本");
		
		// 実行
		List<CountryCodeModel> countryList = countryCodeService.getCountryList(locale);
		
		// 確認
		assertAll(
			() -> assertEquals(2, countryList.size()),
			() -> assertEquals("中国", countryList.get(0).getName()),
			() -> assertEquals("+86", countryList.get(0).getCountryCode()),
			() -> assertEquals("日本", countryList.get(1).getName()),
			() -> assertEquals("+81", countryList.get(1).getCountryCode())
		);
	}
}
