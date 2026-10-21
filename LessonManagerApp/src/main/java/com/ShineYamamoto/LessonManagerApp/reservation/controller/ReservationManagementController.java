package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.reservation.view.ReservationView;

@Controller
@RequestMapping("/user/reservation")
public class ReservationManagementController {

	private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
	
	private static final int MAX_DISPLAY_COUNT = 50;
	
	// 画面確認の予約件数
	private static final int SAMPLE_COUNT = 10;
	
	// 後で管理者設定を取得処理へ置き換える予定
	private static final int CHANGE_DEADLINE_HOURS = 3;
	private static final int CHANGE_LEAD_HOURS = 24;
	private static final int CANCEL_DEADLINE_HOURS = 6;
	
	/**
	 * このControllerの各画面で使う設定を渡す
	 */
	@ModelAttribute("reservationPolicy")
	public Map<String, Integer> reservationPolicy() {
		
		return Map.of(
				"changeDeadlineHours", CHANGE_DEADLINE_HOURS,
				"changeLeadHours", CHANGE_LEAD_HOURS,
				"cancelDeadlineHours", CANCEL_DEADLINE_HOURS);
	}
	
	/**
	 * 予約確認画面
	 */
	@GetMapping("/list")
	public String getReservationList(Model model) {
		
		LocalDateTime now = LocalDateTime.now(JAPAN_ZONE);
		
		model.addAttribute("reservations", createSampleReservations(now));
		
		return "reservation/reservation-list";
	}
	
	/**
	 * 画面確認用のサンプルデータ
	 * いずれ本人確認ようのDB取得する処理に置き換える
	 */
	private List<ReservationView> createSampleReservations(
			LocalDateTime now) {
		
		List<ReservationView> reservations = new ArrayList<>();
		
		LocalDate today = now.toLocalDate();
		
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			
			// 明日から1日おきに13:00～14:00の予約を作る
			LocalDateTime startsAt = today.plusDays(i + 1).atTime(13, 0);
			
			LocalDateTime endsAt = startsAt.plusMinutes(60);
			
			boolean editable = !startsAt.isBefore(
					now.plusHours(CHANGE_DEADLINE_HOURS));
			
			boolean cancellable = !startsAt.isBefore(
					now.plusHours(CANCEL_DEADLINE_HOURS));
			
			reservations.add(new ReservationView(
					(long) i + 1,
					startsAt,
					endsAt,
					editable,
					cancellable));
		}
		
		return reservations.stream()
				.sorted(java.util.Comparator.comparing(ReservationView::getStartsAt))
				.limit(MAX_DISPLAY_COUNT)
				.toList();
	}
}
