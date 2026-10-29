package com.ShineYamamoto.LessonManagerApp.reservation.domain.model;

import java.time.LocalTime;

import lombok.Data;

@Data
public class ReservationSettings {

	private Integer settingsId;
	
	private LocalTime selectableStartTime;
	
	private LocalTime selectableEndTime;
}
