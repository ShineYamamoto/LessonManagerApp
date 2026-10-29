package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationMapper;
import com.ShineYamamoto.LessonManagerApp.reservation.view.CalendarDayAvailability;


@SpringBootTest
public class ReservationCalendarServiceTest {
	
	@Autowired
	private ReservationCalendarService calendarService;
	
	@Autowired
	private ReservationMapper reservationMapper;
	
	@Test
	void 月単位で受付可能時間を取得できる() {
		
		var result = calendarService.getMonth(YearMonth.of(2026, 11), null);
		
		var day = result.get("2026-11-10");
		
		assertEquals(
				new CalendarDayAvailability.MinuteRange(540, 840),
				day.openings().get(0));
	}
	
	@Test
	void 確定予約を取得でき変更対象のみ除外できる() {
		
		LocalDateTime from = LocalDateTime.of(2026, 12, 1, 0, 0);
		
		LocalDateTime to = from.plusDays(1);
		
		Long reservattionId = reservationMapper.findConfirmedOverlapping(from, to)
											   .get(0)
											   .getReservationId();
		
		var normal = calendarService.getMonth(YearMonth.of(2026, 12), null);
		var editing = calendarService.getMonth(YearMonth.of(2026, 12), reservattionId);
		
		assertEquals(1, normal.get("2026-12-01").booked().size());
		
		assertTrue(editing.get("2026-12-01").booked().isEmpty());
	}
}
