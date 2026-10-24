package com.ShineYamamoto.LessonManagerApp.reservation.form;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class ReservationChangeForm {
	
	/** 予約希望の開始日。日本時間として扱う */
	@NotNull
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime startsAt;
	
	/** 編集画面を開いた時点の更新番号 */
	@NotNull
	@Min(0)
	private Long expectedVersion;
}
