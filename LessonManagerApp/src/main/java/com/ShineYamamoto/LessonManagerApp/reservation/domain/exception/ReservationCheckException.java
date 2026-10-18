package com.ShineYamamoto.LessonManagerApp.reservation.domain.exception;

import java.util.List;

/** 予約条件を満たしていない場合の例外 */
/** ServiceからControllerへ「予約できなかった理由」を伝える */
public class ReservationCheckException extends RuntimeException {
	
	private final List<ReservationProblem> problems;
	
	/** 予約日時を特定できないエラー */
	public ReservationCheckException(String message) {
		super(message);
		this.problems = List.of();
	}
	
	/** 問題となった予約日時をまとめて保持する */
	public ReservationCheckException(
			List<ReservationProblem> problems) {
		
		super("予約条件を満たさない日時があります。");
		
		this.problems = List.copyOf(problems);
	}
	
	public List<ReservationProblem> getProblems() {
		return problems;
	}
}
