package com.ShineYamamoto.LessonManagerApp.reservation.domain.exception;

/** 予約条件を満たしていない場合の例外 */
/** ServiceからControllerへ「予約できなかった理由」を伝える */
public class ReservationCheckException extends RuntimeException {

	public ReservationCheckException(String message) {
		super(message);
	}
}
