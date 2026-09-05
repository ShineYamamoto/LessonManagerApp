package com.ShineYamamoto.LessonManagerApp.common.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ShineYamamoto.LessonManagerApp.common.model.CountryCodeModel;

@Service
public class CountryCodeService {
	
	public List<CountryCodeModel> getCountryList() {
		
		return List.of(
			new CountryCodeModel("中国", "+86", "13812345678"),
			new CountryCodeModel("日本", "+81", "09012345678")
		);
	}
}
