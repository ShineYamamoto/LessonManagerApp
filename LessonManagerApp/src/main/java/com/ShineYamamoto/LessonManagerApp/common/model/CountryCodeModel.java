package com.ShineYamamoto.LessonManagerApp.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CountryCodeModel {
	private String name;
	private String countryCode;
	private String phoneExample;
}
