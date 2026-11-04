package com.ShineYamamoto.LessonManagerApp.reservation.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 読み取り専用画面に渡すデータ。record作成後に変更しない */
public record LessonOpeningTimelineView(
		String month,
		String monthLabel,
		String previousMonth,
		String nextMonth,
		String startLabel,
		String endLabel,
		int slotCount,
		int openingCount,
		int reservationCount,
		boolean hasClippedItems,
		List<Tick> ticks,
		List<Day> days) {
	
	// 横軸の時刻ラベル。
	public record Tick(
			String label,
			String positionStyle,
			String edgeClass) {
		
	}
	
	// 1日分。受付がない日も必ず1件作る
	public record Day(
			LocalDate date,
			String dateLabel,
			String weekday,
			boolean today,
			boolean weekend,
			boolean hasClippedItems,
			List<Bar> openings,
			List<Bar> reservations) {
		
	}
	
	// 受付枠または予約の表示情報。元の開始・終了日時を維持
	public record Bar(
			Long id,
			Long version,
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			String startLabel,
			String endLabel,
			String description,
			boolean visible,
			boolean clipped,
			boolean compact,
			String positionStyle) {
		
	}
}
