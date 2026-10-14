package com.ShineYamamoto.LessonManagerApp.reservation.form;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class ReservationItemForm {
	
	/** 予約希望の開始日。日本時間として扱う */
	@NotNull
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime startsAt;
}
