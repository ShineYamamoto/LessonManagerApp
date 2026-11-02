package com.ShineYamamoto.LessonManagerApp.reservation.form;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class LessonOpeningForm {

	@NotNull(message = "{lessonOpening.validation.date}")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate date;
	
	@NotNull(message = "{lessonOpening.validation.startTime}")
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime startTime;
	
	@NotNull(message = "{lessonOpening.validation.endTime}")
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime endTime;
	
	// 新規登録では不要
	@Min(value = 0, message = "{lessonOpening.validation.version}")
	private Long expectedVersion;
}
