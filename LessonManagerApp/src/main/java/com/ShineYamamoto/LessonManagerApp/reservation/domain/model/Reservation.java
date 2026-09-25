package com.ShineYamamoto.LessonManagerApp.reservation.domain.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Reservation {

	private Long reservationId;
	
	private Long userId;
	
	private LocalDateTime startsAt;
	
	private LocalDateTime endsAt;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
}
