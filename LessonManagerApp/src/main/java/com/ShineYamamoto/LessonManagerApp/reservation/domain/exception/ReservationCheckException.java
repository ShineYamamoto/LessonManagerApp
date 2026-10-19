package com.ShineYamamoto.LessonManagerApp.reservation.domain.exception;

import java.util.List;

/** 予約条件を満たしていない場合の例外 */
/** ServiceからControllerへ「予約できなかった理由」を伝える */
public class ReservationCheckException extends RuntimeException {
	
	private final String messageKey;
	
	private final List<ReservationProblem> problems;
	
	/** 予約日時を特定できないエラー */
	public ReservationCheckException(String messageKey) {
		super(messageKey);
		
		this.messageKey = messageKey;
		this.problems = List.of();
	}
	
	/** 問題となった予約日時をまとめて保持する */
	public ReservationCheckException(
			List<ReservationProblem> problems) {
		
		super("reservation.error.invalidSlots");
		
		this.messageKey = "reservation.error.invalidSlots";
		this.problems = List.copyOf(problems);
	}
	
	public String getMessageKey() {
		return messageKey;
	}
	
	public List<ReservationProblem> getProblems() {
		return problems;
	}
}
