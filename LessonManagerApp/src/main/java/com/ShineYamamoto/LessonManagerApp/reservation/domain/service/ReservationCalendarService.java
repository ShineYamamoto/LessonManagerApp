package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.view.CalendarDayAvailability;
import com.ShineYamamoto.LessonManagerApp.reservation.view.CalendarDayAvailability.MinuteRange;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationCalendarService {

	private static final LocalTime OPEN = LocalTime.of(9, 0);
	private static final LocalTime CLOSE = LocalTime.of(22, 0);
	
	private final ReservationDataService reservationDataService;
	
	/**
	 * 指定付きのカレンダー表示用データを取得する
	 * 
	 * excludedReservationId:
	 * 通常予約ではnull
	 * 予約変更では変更対象の予約ID
	 */
	public Map<String, CalendarDayAvailability> getMonth(
			YearMonth month,
			Long excludedReservationId) {
		
		LocalDate firstDay = month.atDay(1);
		LocalDate endDay = month.plusMonths(1).atDay(1);
		
		LocalDateTime from = firstDay.atStartOfDay();
		LocalDateTime to =endDay.atStartOfDay();
		
		// 月単位でDBから取得する
		List<LessonOpening> openings = reservationDataService.getOpeningsBetween(from, to);
		
		List<Reservation> reservations = reservationDataService.getConfirmedReservationsBetween(from, to);
		
		Map<String, CalendarDayAvailability> result = new LinkedHashMap<>();
		
		// 1件ずつ表示用の情報へ変換
		for (LocalDate date = firstDay; date.isBefore(endDay); date.plusDays(1)) {
			
			LocalDateTime dayStart = date.atStartOfDay();
			LocalDateTime displayStart = date.atTime(OPEN);
			LocalDateTime displayEnd = date.atTime(CLOSE);
			
			// 該当日かどうかの判定はtoRangeでおこなう
			List<MinuteRange> dayOpenings = openings.stream()
					.map(opening -> toRange(
							opening.getStartsAt(),
							opening.getEndsAt(),
							dayStart,
							displayStart,
							displayEnd))
					.filter(Objects::nonNull)
					.toList();
			
			List<MinuteRange> dayBooked = reservations.stream()
					// 予約変更時は変更対象を除外する
					.filter(reservation -> !Objects.equals(reservation.getReservationId(), excludedReservationId))
					.map(reservation -> toRange(
							reservation.getStartsAt(),
							reservation.getEndsAt(),
							dayStart,
							displayStart,
							displayEnd))
					.filter(Objects::nonNull)
					.toList();
			
			result.put(
					date.toString(),
					new CalendarDayAvailability(dayOpenings, dayBooked));
		}
		
		return result;
	}
	
	/**
	 * 日時を午前0時からの経過分数に変換する
	 * 表示範囲の外側は切り詰める
	 */
	private MinuteRange toRange(
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			LocalDateTime dayStart,
			LocalDateTime displayStart,
			LocalDateTime displayEnd) {
		
		// 表示時間より前から受付可能に指定されていた場合は表示開始時間をstartにする
		LocalDateTime start = startsAt.isBefore(displayStart) ? displayStart : startsAt;
		
		// 表示時間より後に受付終了に指定されていた場合は表示終了時間をendにする
		LocalDateTime end = endsAt.isAfter(displayEnd) ? displayEnd : endsAt;
		
		if (!start.isBefore(end)) {
			return null;
		}
		
		int startMinutes = (int) Duration.between(dayStart, start).toMinutes();
		
		int endMinutes = (int) Duration.between(dayStart, end).toMinutes();
		
		return new MinuteRange(startMinutes, endMinutes);
	}
	
}
