package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationForm;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationItemForm;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

@Controller
@RequestMapping("/user")
public class ReservationController {
	
	/** ホーム画面を表示 */
	@GetMapping("/reservation/single")
	public String getReservation() {

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
		
		// 受信確認用の表示内容を作成
		StringBuilder message = new StringBuilder();
		message.append("予約希望日時を受信しました。\n");
		message.append("※DB二は保存していません。\n\n");
		message.append("ログインユーザーID: ")
			   .append(userId)
			   .append("\n");
		
		message.append("受信件数: ")
			   .append(form.getReservationList().size())
			   .append("件\n");
		
		for (ReservationItemForm item : form.getReservationList()) {
			message.append("開始日時（日本時間）: ")
				   .append(item.getStartsAt())
				   .append("\n");
		}
		
		return ResponseEntity.ok(message.toString());
	}
}
