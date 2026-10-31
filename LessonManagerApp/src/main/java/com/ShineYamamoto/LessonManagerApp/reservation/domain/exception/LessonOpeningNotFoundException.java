package com.ShineYamamoto.LessonManagerApp.reservation.domain.exception;

public class LessonOpeningNotFoundException extends RuntimeException {

	public LessonOpeningNotFoundException(Long openingId) {
		
		super("対象の受付可能時間が見つかりません。ID: " + openingId);
	}
}
