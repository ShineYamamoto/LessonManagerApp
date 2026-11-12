package com.ShineYamamoto.LessonManagerApp.reservation.domain.exception;

import java.util.List;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpeningBatchError;

public class LessonOpeningBatchException extends RuntimeException {

	private final List<LessonOpeningBatchError> errors;
	
	public LessonOpeningBatchException(
			List<LessonOpeningBatchError> errors) {
		super("受付枠の一括保存に失敗しました。");
		this.errors = List.copyOf(errors);
	}
	
	public List<LessonOpeningBatchError> getErrors() {
		return errors;
	}
}
