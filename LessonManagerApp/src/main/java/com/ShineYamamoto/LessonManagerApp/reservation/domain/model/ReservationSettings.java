package com.ShineYamamoto.LessonManagerApp.reservation.domain.model;

import java.time.LocalTime;

import lombok.Data;

@Data
public class ReservationSettings {

	private Integer settingsId;
	
	private LocalTime selectableStartTime;
	
	private LocalTime selectableEndTime;
	
	/** 選択開始時刻を、午前0時からの経過分数で返す */
	public int getSelectableStartMinutes() {
		return selectableStartTime.toSecondOfDay() / 60;
	}
	
	/** 選択終了時刻を、午前0時からの経過分数で返す */
	public int getSelectableEndMinutes() {
		return selectableEndTime.toSecondOfDay() / 60;
	}
}
