package com.ShineYamamoto.LessonManagerApp.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PhoneNumberModel {
	
	private String countryCode;
	
	private String regionCode;
	
	private String phoneNumber;
}
