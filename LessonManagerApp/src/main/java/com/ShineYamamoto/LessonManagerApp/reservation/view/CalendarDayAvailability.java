package com.ShineYamamoto.LessonManagerApp.reservation.view;

import java.util.List;

// Spring BootのJacksonがこのrecordをJSONへ変換する
public record CalendarDayAvailability(
		List<MinuteRange> openings,
		List<MinuteRange> booked) {

	public record MinuteRange(
			int start,
			int end) {
		
	}
}
