package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
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
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationCheckService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationDataService;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationForm;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationItemForm;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class ReservationController {
	
	private final ReservationDataService reservationDataService;
	
	private final ReservationCheckService reservationCheckService;
	
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
	@PostMapping(
		value = "/reservation/single",
		produces = "text/plain;charset=UTF-8"
	)
	public ResponseEntity<String> postReservation(
			@Valid @ModelAttribute ReservationForm form,
			BindingResult bindingResult,
			@AuthenticationPrincipal LoginUser loginUser) {
		
		// バリデーションエラーチェック
		if (bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body(
					"予約希望日時を受信できませんでした。"
					+ "1件以上選択し、日時の形式を確認してください。"
			);
		}
		
		// ユーザーIDをログイン情報から取得する
		Long userId = loginUser.getUserId();
		
		// Serviceには画面専用Formではなく、必要な開始日時を渡す
		// フォーム内の各予約候補から「開始日時」だけを取り出し、開始日時のリストを作る
		List<LocalDateTime> requestedStarts = 
				form.getReservationList().stream()
						.map(ReservationItemForm::getStartsAt) // 並べ替え
						.toList();
		
		try {
			
			List<ReservationSlot> checkedSlots = reservationCheckService.check(userId, requestedStarts);
			
			StringBuilder message = new StringBuilder();
			
			message.append("予約希望日時を受信しました。\n");
			message.append("※DB二は保存していません。\n\n");
			
			message.append("チェック件数: ")
				   .append(checkedSlots.size())
				   .append("件\n");
			
			for (ReservationSlot slot : checkedSlots) {
				message.append("日時（日本時間）: ")
					   .append(slot.getStartsAt())
					   .append(" ～ ")
					   .append(slot.getEndsAt())
					   .append("\n");
			}
			
			return ResponseEntity.ok(message.toString());
			
		} catch (ReservationCheckException e) {
			
			return ResponseEntity.badRequest().body(
					"予約出来ませんでした。\n" + e.getMessage());
			
		}
	}
}
