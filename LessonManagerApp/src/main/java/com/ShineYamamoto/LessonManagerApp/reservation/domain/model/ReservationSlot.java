package com.ShineYamamoto.LessonManagerApp.reservation.domain.model;

import java.time.LocalDateTime;

import lombok.Value;

/** サーバー側で計算した予約の開始・終了日時 */
/** 判定に使う時間帯を表す用 */
@Value
public class ReservationSlot {

	LocalDateTime startsAt;
	
	LocalDateTime endsAt;
}
