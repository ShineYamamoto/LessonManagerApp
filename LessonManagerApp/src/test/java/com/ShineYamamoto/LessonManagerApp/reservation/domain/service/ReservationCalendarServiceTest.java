package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSettings;
import com.ShineYamamoto.LessonManagerApp.reservation.view.CalendarDayAvailability.MinuteRange;

@ExtendWith(MockitoExtension.class)
public class ReservationCalendarServiceTest {
	
	@Mock
	private ReservationDataService reservationDataService;
	
	@Mock
	private ReservationSettingsService reservationSettingsService;
	
	@InjectMocks
	private ReservationCalendarService calendarService;
	
	@BeforeEach
	void setUp() {
		
		ReservationSettings settings = new ReservationSettings();
		settings.setSelectableStartTime(LocalTime.of(9,  0));
		settings.setSelectableEndTime(LocalTime.of(22, 0));
		
		when(reservationSettingsService.getSettings())
				.thenReturn(settings);
	}
	
	@Test
	void 月単位で受付可能時間を取得できる() {
		
		LocalDateTime start = LocalDateTime.of(2026, 11, 10, 9, 0);
		LocalDateTime end = LocalDateTime.of(2026, 11, 10, 14, 0);
		
		LessonOpening opening = new LessonOpening();
		opening.setStartsAt(start);
		opening.setEndsAt(end);
		
		LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 12, 1, 0, 0);
		
		when(reservationDataService.getOpeningsBetween(from, to))
				.thenReturn(List.of(opening));
		
		when(reservationDataService.getConfirmedReservationsBetween(from, to))
				.thenReturn(List.of());
		
		var result = calendarService.getMonth(YearMonth.of(2026, 11), null);
		
		var day = result.get("2026-11-10");
		
		assertNotNull(day);
		
		assertEquals(List.of(new MinuteRange(540, 840)), day.openings());
	}
	
	@Test
	void 確定予約を取得でき変更対象のみ除外できる() {
		
		Reservation reservation = new Reservation();
		reservation.setReservationId(100L);
		reservation.setStartsAt(LocalDateTime.of(2026, 12, 1, 10, 0));
		reservation.setEndsAt(LocalDateTime.of(2026, 12, 1, 11, 0));
		
		LocalDateTime from = LocalDateTime.of(2026, 12, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2027, 1, 1, 0, 0);
		
		when(reservationDataService.getOpeningsBetween(from, to))
				.thenReturn(List.of());
		
		when(reservationDataService.getConfirmedReservationsBetween(from, to))
				.thenReturn(List.of(reservation));
		
		var normal = calendarService.getMonth(YearMonth.of(2026, 12), null);
		
		var editing = calendarService.getMonth(YearMonth.of(2026, 12), 100L);
		
		assertEquals(List.of(new MinuteRange(600, 660)), normal.get("2026-12-01").booked());
		
		assertTrue(editing.get("2026-12-01").booked().isEmpty());
	}
}
