package com.ShineYamamoto.LessonManagerApp.reservation.domain.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Value;

@Value
public class ReservationChangeResult implements Serializable {

	private static final long serialVersionID = 1L;
	
	Long reservationId;
	
	Long userId;
	
	/** 更新成功後の更新番号 */
	Long version;
	
	LocalDateTime beforeStartsAt;
	
	LocalDateTime beforeEndsAt;
	
	LocalDateTime afterStartsAt;
	
	LocalDateTime afterEndsAt;
}
