package com.ShineYamamoto.LessonManagerApp.reservation.domain.exception;

public class LessonOpeningCheckException extends RuntimeException {

	private final String messageKey;
	
	public LessonOpeningCheckException(String messageKey) {
		super (messageKey);
		this.messageKey = messageKey;
	}
	
	public String getMessageKey() {
		return messageKey;
	}
}
