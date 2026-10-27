package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import static com.ShineYamamoto.LessonManagerApp.reservation.domain.policy.ReservationPolicy.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationCalendarService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationDataService;
import com.ShineYamamoto.LessonManagerApp.reservation.view.CalendarDayAvailability;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/reservation")
@RequiredArgsConstructor
public class ReservationCalendarController {

	private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
	
	private final ReservationCalendarService calendarService;
	
	private final ReservationDataService reservationDataService;
	
	/** 通常予約画面のカレンダー */
	@GetMapping("/calendar")
	public Map<String, CalendarDayAvailability> getCalendar(
			@RequestParam("month") String month) {
		
		return calendarService.getMonth(
				parseMonth(month),
				null);
	}
	
	/** 予約変更画面のカレンダー */
	@GetMapping("/{reservationId}/calendar")
	public Map<String, CalendarDayAvailability> getEditCalendar(
			@PathVariable Long reservationId,
			@RequestParam("month") String month,
			@AuthenticationPrincipal LoginUser loginUser) {
		
		LocalDateTime now = LocalDateTime.now(JAPAN_ZONE);
		
		// ログイン本人の予約であることを確認する
		Reservation original = reservationDataService.getUpcomingReservation(
					loginUser.getUserId(),
					reservationId,
					now)
				.orElseThrow(() ->
						new ResponseStatusException(
								HttpStatus.NOT_FOUND,
								"対象の予約が見つかりません。"));
		
		// 変更期限を確認
		if (original.getStartsAt().isBefore(now.plusHours(CHANGE_DEADLINE_HOURS))) {
			
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"この予約は変更期限を過ぎています。");
		}
		
		return calendarService.getMonth(parseMonth(month), reservationId);
				
	}
	
	/** yyyy-MM形式の文字列をYearMonthへ変換 */
	private YearMonth parseMonth(String month) {
		
		try {
			return YearMonth.parse(month);
		} catch (DateTimeParseException e) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"年月の形式が正しくありません。",
					e);
		}
	}
}
