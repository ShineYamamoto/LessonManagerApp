package com.ShineYamamoto.LessonManagerApp.common.service;

import org.springframework.stereotype.Service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

@Service
public class PhoneNumberService {
	
	private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
	
	/** 入力された電話番号をE.164形式に変換する */
	public String toE164 (
			String regionCode,
			String phoneNumber) {
		
		try {
			
			PhoneNumber parsedNumber = 
					phoneNumberUtil.parse(phoneNumber, regionCode);
			
			if (!phoneNumberUtil.isValidNumber(parsedNumber)) {
				throw new IllegalArgumentException(
						"電話番号が正しくありません");
			}
			
			return phoneNumberUtil.format(
					parsedNumber,
					PhoneNumberUtil.PhoneNumberFormat.E164);
					
			
			
		} catch (NumberParseException e) {
			
			throw new IllegalArgumentException(
					"電話番号を解析できません",
					e);
		}
	}
}
