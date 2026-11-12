package com.ShineYamamoto.LessonManagerApp.reservation.domain.model;

public record LessonOpeningBatchError (
	String clientId,
	String field,
	String messageKey) {
	
}
