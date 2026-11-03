package com.ShineYamamoto.LessonManagerApp.reservation.form;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class LessonOpeningDeleteForm {

	@NotNull(message = "{lessonOpening.validation.version}")
	@Min(value = 0, message = "{lessonOpening.validation.version}")
	private Long expectedVersion;
	
	// 削除処理後に戻る一覧の日付
	@NotNull(message = "{lessonOpening.validation.date}")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate date;
}
