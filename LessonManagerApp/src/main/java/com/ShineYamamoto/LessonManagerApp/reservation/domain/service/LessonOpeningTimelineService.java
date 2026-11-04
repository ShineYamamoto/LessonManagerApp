package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSettings;
import com.ShineYamamoto.LessonManagerApp.reservation.view.LessonOpeningTimelineView;
import com.ShineYamamoto.LessonManagerApp.reservation.view.LessonOpeningTimelineView.Bar;
import com.ShineYamamoto.LessonManagerApp.reservation.view.LessonOpeningTimelineView.Day;
import com.ShineYamamoto.LessonManagerApp.reservation.view.LessonOpeningTimelineView.Tick;

@Service
@Transactional(readOnly = true)
public class LessonOpeningTimelineService {

	private static final int STEP_MINUTES = 30;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("M/d HH:mm");
	
	private final ReservationDataService reservationDataService;
	private final ReservationSettingsService reservationSettingsService;
	
	public LessonOpeningTimelineService(
			ReservationDataService reservationDataService,
			ReservationSettingsService reservationSettingsService) {
		this.reservationDataService = reservationDataService;
		this.reservationSettingsService = reservationSettingsService;
	}
	
	public LessonOpeningTimelineView getMonth(YearMonth month, LocalDate today) {
		
		ReservationSettings settings = reservationSettingsService.getSettings();
		
		LocalDate firstDay = month.atDay(1);
		LocalDate nextFirstDay = month.plusMonths(1).atDay(1);
		LocalDateTime from = firstDay.atStartOfDay();
		LocalDateTime to = nextFirstDay.atStartOfDay();
		
		// 対象月のデータをまとめて取得する
		List<LessonOpening> openings = reservationDataService.getOpeningsBetween(from, to);
		List<Reservation> reservations = reservationDataService.getConfirmedReservationsBetween(from, to);
		
		int startMinutes = settings.getSelectableStartMinutes();
		int endMinutes = settings.getSelectableEndMinutes();
		int slotCount = (endMinutes - startMinutes) / STEP_MINUTES;
		
		List<Day> days = new ArrayList<>();
		for (LocalDate date = firstDay; date.isBefore(nextFirstDay); date = date.plusDays(1)) {
			LocalDateTime dayStart = date.atStartOfDay();
			LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
			LocalDateTime displayStart = date.atTime(settings.getSelectableStartTime());
			LocalDateTime displayEnd = date.atTime(settings.getSelectableEndTime());
			
			List<Bar> dayOpenings = openings.stream()
					.filter(item -> overlaps(item.getStartsAt(), item.getEndsAt(), dayStart, dayEnd))
					.map(item -> toBar(item.getOpeningId(), item.getVersion(), item.getStartsAt(), item.getEndsAt(), displayStart, displayEnd, "受付可能"))
					.toList();
			
			List<Bar> dayReservations = reservations.stream()
					.filter(item -> overlaps(item.getStartsAt(), item.getEndsAt(), dayStart, dayEnd))
					.map(item -> toBar(item.getReservationId(), item.getVersion(), item.getStartsAt(), item.getEndsAt(), displayStart, displayEnd, "確定予約"))
					.toList();
			
			boolean clipped = dayOpenings.stream().anyMatch(Bar::clipped) || dayReservations.stream().anyMatch(Bar::clipped);
			DayOfWeek weekday = date.getDayOfWeek();
			
			days.add(new Day(
					date,
					date.format(DateTimeFormatter.ofPattern("M/d")),
					weekday.getDisplayName(TextStyle.SHORT, Locale.JAPANESE),
					date.equals(today),
					weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY,
					clipped,
					dayOpenings,
					dayReservations));
		}
		
		return new LessonOpeningTimelineView(
				month.toString(),
				month.format(DateTimeFormatter.ofPattern("yyyy年M月")),
				month.equals(YearMonth.of(1900, 1)) ? null : month.minusMonths(1).toString(),
				month.equals(YearMonth.of(9999, 12)) ? null : month.plusMonths(1).toString(),
				settings.getSelectableStartTime().format(TIME),
				settings.getSelectableEndTime().format(TIME),
				slotCount,
				openings.size(),
				reservations.size(),
				days.stream().anyMatch(Day::hasClippedItems),
				buildTicks(startMinutes, endMinutes),
				List.copyOf(days));
	}
	
	/** 
	 * 該当する日の受付可能時間のみ取得
	 * 終了と開始が接するだけの場合は重なりに含めない */
	private boolean overlaps(
			LocalDateTime start,
			LocalDateTime end,
			LocalDateTime from,
			LocalDateTime to) {
		
		return start.isBefore(to) && end.isAfter(from);
	}
	
	private Bar toBar(
			Long id,
			Long version,
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			LocalDateTime displayStart,
			LocalDateTime displayEnd,
			String kind) {
		
		// 元の日時は保持し、描画する範囲だけをの時間帯を切り抜く
		LocalDateTime visibleStart = startsAt.isBefore(displayStart) ? displayStart : startsAt;
		LocalDateTime visibleEnd = endsAt.isAfter(displayEnd) ? displayEnd : endsAt;
		boolean visible = visibleStart.isBefore(visibleEnd);
		boolean clipped = startsAt.isBefore(displayStart) || endsAt.isAfter(visibleEnd);
		
		String style = "left:0%;width:0%;";
		if (visible) {
			double totalSeconds = Duration.between(displayStart, displayEnd).toSeconds();
			// バーの開始位置（%）（表示開始から枠の開始までの長さ ÷ 表示時間全体の長さ）×100。
			double left = Duration.between(displayStart, visibleStart).toSeconds() / totalSeconds * 100;
			// バーの長さ（%）（枠の長さ ÷ 表示時間全体の長さ）×100。
			double width = Duration.between(visibleStart, visibleEnd).toSeconds() / totalSeconds * 100;
			
			// cssの小数点が実行環境のロケールによってカンマになるのを防ぐ
			style = String.format(Locale.ROOT, "left:%.6f%%;width:%.6f%%", left, width);
		}
		
		String description = kind + " " + startsAt.format(DATE_TIME) 
								+ "～" + endsAt.format(DATE_TIME)
								+ (clipped ? "（表示範囲外の時間を含みます）" : "");
		
		return new Bar(
				id, version, startsAt, endsAt,
				startsAt.format(TIME), endsAt.format(TIME), description,
				visible, clipped,
				visible && Duration.between(displayStart, visibleEnd).toMinutes() < 60,
				style);
		
	}
	
	private List<Tick> buildTicks(int startMinutes, int endMinutes) {
		
		List<Tick> ticks = new ArrayList<>();
		for (int minute = startMinutes; minute <= endMinutes; minute += STEP_MINUTES) {
			
			// 両端と毎正時だけ文字を表示。30分の区切りはcssの罫線で示す
			if (minute != startMinutes && minute != endMinutes && minute % 60 != 0) {
				continue;
			}
			double left = (minute - startMinutes) * 100.0 / (endMinutes - startMinutes);
			String edgeClass = minute == startMinutes ? "lot-tick-first" : minute == endMinutes ? "lot-tick-last" : "";
			
			ticks.add(new Tick(
					LocalTime.of(minute / 60, minute % 60).format(TIME),
					String.format(Locale.ROOT, "left:%.6f%%;", left),
					edgeClass));
			
		}
		return List.copyOf(ticks);
	}
}
