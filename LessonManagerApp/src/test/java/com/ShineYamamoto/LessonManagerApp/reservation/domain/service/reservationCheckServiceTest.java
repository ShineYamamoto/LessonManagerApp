package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationProblem;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;

@ExtendWith(MockitoExtension.class)
public class reservationCheckServiceTest {

	private static final Long USER_ID = 2L;
	
	@Mock
	private ReservationDataService reservationDataService;
	
	@InjectMocks
	private ReservationCheckService reservationCheckService;
	
	private LocalDate date;
	
	@BeforeEach
	void setUp() {
		// 過去日時にならないよう動的に決定
		date = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(60);
	}
	
	@Test
	void 正常な2件の予約は終了日時まで計算される() {
		
		when(reservationDataService.getLessonMinutes(USER_ID))
			.thenReturn(60);
		
		// 9時から13時まで受付可能
		when(reservationDataService.getOpening(date))
			.thenReturn(List.of(opening(9, 13)));
		
		when(reservationDataService.getConfirmedReservation(date))
			.thenReturn(List.of());
		
		List<ReservationSlot> result = reservationCheckService.check(USER_ID, List.of(at(10), at(11)));
		
		assertEquals(2, result.size());
		
		assertEquals(at(10), result.get(0).getStartsAt());
		assertEquals(at(11), result.get(0).getEndsAt());
		
		assertEquals(at(11), result.get(1).getStartsAt());
		assertEquals(at(12), result.get(1).getEndsAt());
		
		// 同じ日のデータを何度も取得していない
		verify(reservationDataService, times(1)).getOpening(date);
	}
	
	@Test
	void 予約済みと受付時間外の両方を検出できる() {
		
		when(reservationDataService.getLessonMinutes(USER_ID))
			.thenReturn(60);
		
		// 9時から13時まで受付可能
		when(reservationDataService.getOpening(date))
			.thenReturn(List.of(opening(9, 13)));
		
		// 10時から11字は予約済み
		Reservation booked = new Reservation();
		booked.setStartsAt(at(10));
		booked.setEndsAt(at(11));
		
		when(reservationDataService.getConfirmedReservation(date))
			.thenReturn(List.of(booked));
		
		// 10時: 予約済み
		// 11時: 正常
		// 14時: 受付時間外
		List<LocalDateTime> requestedStarts = List.of(at(10), at(11), at(14));
		
		// 正しく例外が発生するか検証
		ReservationCheckException exception = assertThrows(
				ReservationCheckException.class,
				() -> reservationCheckService.check(USER_ID, requestedStarts));
		
		Map<LocalDateTime, String> errors = exception.getProblems().stream()
				.collect(Collectors.toMap(
						ReservationProblem::startsAt,
						ReservationProblem::messageKey));
		
		assertEquals(2, errors.size());
		
		assertEquals("reservation.error.booked", errors.get(at(10)));
		
		assertEquals("reservation.error.outsideOpening", errors.get(at(14)));
		
		// 正常な11字はエラーに含まれない
		assertFalse(errors.containsKey(at(11)));
	}
	
	// ----------- テスト用の共通処理 --------------
	private LocalDateTime at(int hour) {
		return date.atTime(hour, 0);
	}
	
	private LessonOpening opening(int from, int to) {
		LessonOpening opening = new LessonOpening();
		opening.setStartsAt(at(from));
		opening.setEndsAt(at(to));
		return opening;
	}
}
