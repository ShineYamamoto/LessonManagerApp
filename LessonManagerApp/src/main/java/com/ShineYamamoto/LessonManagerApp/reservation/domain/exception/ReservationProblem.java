package com.ShineYamamoto.LessonManagerApp.reservation.domain.exception;

import java.time.LocalDateTime;

/** 予約できなかった開始日時とその理由 */
public record ReservationProblem(
	LocalDateTime startsAt,
	String messageKey) {
}
