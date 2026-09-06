package com.ShineYamamoto.LessonManagerApp.common.service;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.ShineYamamoto.LessonManagerApp.common.model.CountryCodeModel;

@Service
public class CountryCodeService {
	
	private final MessageSource messageSource;
	
	public CountryCodeService(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	public List<CountryCodeModel> getCountryList(Locale locale) {
		
		return List.of(
			new CountryCodeModel(
					messageSource.getMessage("country.china", null, locale),
					"+86",
					"例：13812345678"
			),
			new CountryCodeModel(
					messageSource.getMessage("country.japan", null, locale),
					"+81",
					"例：09012345678"
			)
		);
	}
}
