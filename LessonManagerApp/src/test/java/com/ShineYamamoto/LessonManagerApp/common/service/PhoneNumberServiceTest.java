package com.ShineYamamoto.LessonManagerApp.common.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PhoneNumberServiceTest {

	@Test
	void 日本の携帯電話番号をE164形式に変換できる() {
		
		// 準備
		PhoneNumberService service = new PhoneNumberService();
		
		// 実行
		String result = service.toE164(
				"JP", "09012345678"
		);
		
		assertEquals("+819012345678", result);
	}
}