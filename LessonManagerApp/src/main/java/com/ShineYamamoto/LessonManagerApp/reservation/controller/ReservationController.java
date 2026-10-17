package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationDataService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationService;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationForm;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationItemForm;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class ReservationController {
	
	private final ReservationDataService reservationDataService;
	
	private final ReservationService reservationService;
	
	// セッションに保存する情報のキー
	private static final String COMPLETED_SLOTS_KEY = "reservation.completedSlots";
	private static final String COMPLETED_USER_ID_KEY = "reservation.completedUserId";
	
	/** ホーム画面を表示 */
	@GetMapping("/reservation/single")
	public String getReservation(
			@AuthenticationPrincipal LoginUser loginUser,
			Model model) {

		int lessonMinutes = reservationDataService.getLessonMinutes(loginUser.getUserId());
		
		model.addAttribute("lessonMinutes", lessonMinutes);
		
		return "reservation/reservation-single";
	}
	
	/** 予約希望日時を受信する。*/
	@PostMapping("/reservation/single"	)
	public String postReservation(
			@Valid @ModelAttribute ReservationForm form,
			BindingResult bindingResult,
			@AuthenticationPrincipal LoginUser loginUser,
			HttpSession session,
			Model model) {
		
		// ユーザーIDをログイン情報から取得する
		Long userId = loginUser.getUserId();
		
		// 前回の予約完了情報を削除
		session.removeAttribute(COMPLETED_SLOTS_KEY);
		session.removeAttribute(COMPLETED_USER_ID_KEY);
		
		// バリデーションエラーチェック
		if (bindingResult.hasErrors()) {
			return showReservationError(
					userId,
					"予約希望日時を受信できませんでした。"
					+ "1件以上選択し、日時の形式を確認してください。",
					model
			);
		}
		
		

		// Serviceには画面専用Formではなく、必要な開始日時を渡す
		// フォーム内の各予約候補から「開始日時」だけを取り出し、開始日時のリストを作る
		List<LocalDateTime> requestedStarts = 
				form.getReservationList().stream()
						.map(ReservationItemForm::getStartsAt) // 開始日時を取り出す
						.toList();
		
		try {
			
			// サーバー側で検証・DB登録を実行
			List<ReservationSlot> reservedSlots = reservationService.reserve(userId, requestedStarts);
			
			// 予約結果をセッションへ保存
			session.setAttribute(COMPLETED_SLOTS_KEY, List.copyOf(reservedSlots));
			
			// 予約したユーザーも記録する
			session.setAttribute(COMPLETED_USER_ID_KEY, userId);
			
			
			StringBuilder message = new StringBuilder();
			
			return "redirect:/user/reservation/complete";
			
		} catch (ReservationCheckException e) {
			
			// 失敗時の候補復元は後で実装する
			return showReservationError(
					userId,
					"予約できませんでした。"
					+ e.getMessage(),
					model);
	
		}
	}
	
	/** 予約完了画面を表示する */
	@GetMapping("/reservation/complete")
	public String getReservationComplete(
			@AuthenticationPrincipal LoginUser loginUser,
			HttpSession session,
			Model model) {
		
		Object storedUserId = session.getAttribute(COMPLETED_USER_ID_KEY);
		
		Object storedSlots = session.getAttribute(COMPLETED_SLOTS_KEY);
		
		// ログインユーザーと予約者が一致するか確認
		if (!Objects.equals(storedUserId, loginUser.getUserId())) {
			
			return "redirect:/user/reservation/single";
		}
		
		// 予約結果が存在し、正しい型かを確認
		if (!(storedSlots instanceof List<?> slots)
				|| slots.isEmpty()
				|| !slots.stream().allMatch(
						ReservationSlot.class::isInstance)) {
			
			return "redirect:/user/reservation/single";
		}
		
		// Thymeleafへ確定済み予約を渡す
		model.addAttribute("reservedSlots", slots);
		
		return "reservation/reservation-complete";
	}
	
	/** 
	 * 予約エラー時に元の画面を再現する 
	 */
	private String showReservationError(
			Long userId,
			String message,
			Model model) {
		
		int lessonMinutes = reservationDataService.getLessonMinutes(userId);
		
		model.addAttribute("lessonMinutes", lessonMinutes);
		model.addAttribute("reservationError", message);
		
		return "reservation/reservation-single";
	}
}
