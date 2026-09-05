package com.ShineYamamoto.LessonManagerApp.login.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class LoginApplicationService {
	
	/** 国番号のMapを生成する */
	public Map<String, String> getCountryCodeMap() {
		Map<String, String> countryCodeMap = new LinkedHashMap<>();
		countryCodeMap.put("日本", "+81");
		countryCodeMap.put("中国", "+86");
		
		return countryCodeMap;
	}
}
