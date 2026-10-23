package com.ShineYamamoto.LessonManagerApp.reservation.domain.policy;

/** 予約の変更・キャンセルに関する設定 */
public class ReservationPolicy {

	public static final int CHANGE_DEADLINE_HOURS = 3;
	
	public static final int CHANGE_LEAD_HOURS = 24;
	
	public static final int CANCEL_DEADLINE_HOURS = 6;
	
	private ReservationPolicy() {
	}
}
