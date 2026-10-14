package com.ShineYamamoto.LessonManagerApp.reservation.domain.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LessonOpening {

	private Long openingId;
	
	private LocalDateTime startsAt;
	
	private LocalDateTime endsAt;
}
