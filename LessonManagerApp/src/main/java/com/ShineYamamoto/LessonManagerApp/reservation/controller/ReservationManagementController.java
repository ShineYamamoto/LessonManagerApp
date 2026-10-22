package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationDataService;
import com.ShineYamamoto.LessonManagerApp.reservation.view.ReservationView;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/reservation")
@RequiredArgsConstructor
public class ReservationManagementController {
	
	private final ReservationDataService reservationDataService;

	private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
	
	private static final int MAX_DISPLAY_COUNT = 50;
	
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
	public String getReservationList(
			@AuthenticationPrincipal LoginUser loginUser,
			Model model) {
		
		LocalDateTime now = LocalDateTime.now(JAPAN_ZONE);
		
		List<Reservation> reservations = reservationDataService.getUpcomingReservations(
				loginUser.getUserId(),
				now,
				MAX_DISPLAY_COUNT);
		
		List<ReservationView> views = reservations.stream()
				.map(reservation -> toView(reservation, now))
				.toList();
		
		model.addAttribute("reservations", views);
		
		return "reservation/reservation-list";
	}
	
	/**
	 * 予約編集画面
	 */
	@GetMapping("/{reservationId}/edit")
	public String getReservationEdit(
			@PathVariable("reservationId") Long reservationId,
			@AuthenticationPrincipal LoginUser loginUser,
			Model model,
			RedirectAttributes redirectAttributes) {
		
		LocalDateTime now = LocalDateTime.now(JAPAN_ZONE);
		
		Reservation reservation = reservationDataService.getUpcomingReservation(
				loginUser.getUserId(),
				reservationId,
				now)
				.orElseThrow(() ->
						new ResponseStatusException(HttpStatus.NOT_FOUND, "対象の予約が見つかりません。"));
		
		ReservationView view = toView(reservation, now);
		
		// 変更期限を確認する
		if (!view.isEditable()) {
			redirectAttributes.addFlashAttribute(
					"reservationError",
					"この予約の日時変更の受付は終了しました。");
			
			return "redirect:/user/reservation/list";
		}
		
		
		model.addAttribute("reservation", view);
		
		// 実際に予約された予約の授業時間を維持する
		long lessonMinutes = Duration.between(
				reservation.getStartsAt(),
				reservation.getEndsAt()).toMinutes();
		
		model.addAttribute("lessonMinutes", lessonMinutes);
		
		return "reservation/reservation-edit";
	}
	
	/**
	 * DBの予約情報を、画面表示用の情報へ変換する
	 */
	private ReservationView toView(
			Reservation reservation,
			LocalDateTime now) {
		
		boolean editable = !reservation.getStartsAt().isBefore(now.plusHours(CHANGE_DEADLINE_HOURS));
		
		boolean cancellable = !reservation.getStartsAt().isBefore(now.plusHours(CANCEL_DEADLINE_HOURS));
		
		return new ReservationView(
				reservation.getReservationId(),
				reservation.getStartsAt(),
				reservation.getEndsAt(),
				editable,
				cancellable);
	}
}
