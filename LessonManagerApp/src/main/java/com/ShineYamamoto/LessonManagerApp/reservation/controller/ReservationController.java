package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationProblem;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationDataService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationSettingsService;
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
	
	private final MessageSource messageSource;
	
	private final ReservationSettingsService reservationSettingsService;
	
	private static final DateTimeFormatter ERROR_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d HH:mm");
	
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
		
		model.addAttribute("reservationSettings", reservationSettingsService.getSettings());
		
		return "reservation/reservation-single";
	}
	
	/** 予約希望日時を受信する。*/
	@PostMapping("/reservation/single"	)
	public String postReservation(
			@Valid @ModelAttribute ReservationForm form,
			BindingResult bindingResult,
			@AuthenticationPrincipal LoginUser loginUser,
			HttpSession session,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		// ユーザーIDをログイン情報から取得する
		Long userId = loginUser.getUserId();
		
		// 前回の予約完了情報を削除
		session.removeAttribute(COMPLETED_SLOTS_KEY);
		session.removeAttribute(COMPLETED_USER_ID_KEY);
		
		// フォームから変換できた日時を取り出す
		List<LocalDateTime> requestedStarts = extractStarts(form);
		
		// バリデーションエラーチェック
		if (bindingResult.hasErrors()) {
			
			String message = messageSource.getMessage(
					"reservation.error.formInvalid",
					null,
					locale);
			
			return redirectReservationError(
					message,
					requestedStarts,
					redirectAttributes);
		}

		
		try {
			
			// サーバー側で検証・DB登録を実行
			List<ReservationSlot> reservedSlots = reservationService.reserve(userId, requestedStarts);
			
			// 予約結果をセッションへ保存
			session.setAttribute(COMPLETED_SLOTS_KEY, List.copyOf(reservedSlots));
			
			// 予約したユーザーも記録する
			session.setAttribute(COMPLETED_USER_ID_KEY, userId);
			
			return "redirect:/user/reservation/complete";
			
		} catch (ReservationCheckException e) {
			
			// 予約条件に問題があった日時を取得
			Set<LocalDateTime> failedStarts = e.getProblems().stream()
					.map(ReservationProblem::startsAt)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			
			// 問題のない候補だけを残す
			List<LocalDateTime> remainingStarts = requestedStarts.stream()
					.filter(start ->
							!failedStarts.contains(start))
					.toList();
			
			// エラーメッセージを生成
			String errorMessage;
			
			if (e.getProblems().isEmpty()) {
				
				errorMessage = messageSource.getMessage(
						e.getMessageKey(),
						null,
						locale);
				
			} else {
				
				errorMessage = e.getProblems().stream()
						.map(problem -> {
							
							LocalDateTime startsAt = problem.startsAt();
							
							String dateText = 
									startsAt == null
									? ""
									: startsAt.format(ERROR_DATE_FORMAT);
							
							return messageSource.getMessage(
									problem.messageKey(),
									new Object[] {dateText},
									locale);
						})
						.distinct()
						.collect(Collectors.joining("\n"));
	
			}
			
			// 残りの候補を保持して予約画面へ戻る
			return redirectReservationError(
					errorMessage,
					remainingStarts,
					redirectAttributes);
	
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
		
		// 開始日時の早い順に並べ替え
		List<ReservationSlot> sortedSlots = slots.stream()
				.map(ReservationSlot.class::cast)
				.sorted(Comparator.comparing(ReservationSlot::getStartsAt))
				.toList();
		
		// Thymeleafへ確定済み予約を渡す
		model.addAttribute("reservedSlots", sortedSlots);
		
		return "reservation/reservation-complete";
	}
	
	/** フォームから正常に変換できた開始日時を取り出す */
	private List<LocalDateTime> extractStarts(
			ReservationForm form) {
		
		if (form.getReservationList() == null) {
			return List.of();
		}
		
		return form.getReservationList().stream()
				.filter(Objects::nonNull)
				.map(ReservationItemForm::getStartsAt)
				.filter(Objects::nonNull)
				.toList();
	}
	
	/**
	 * エラー内容と残りの候補を保存して予約画面へ戻る
	 */
	private String redirectReservationError(
			String message,
			List<LocalDateTime> remainingStarts,
			RedirectAttributes redirectAttributes) {
		
		// JavaScriptへ渡すために日時を文字列化する
		String restoredStarts = remainingStarts.stream()
				.distinct()
				.map(DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
				.collect(Collectors.joining(","));
		
		// エラーメッセージ
		redirectAttributes.addFlashAttribute(
				"reservationError",
				message);
		
		// 復元する予約候補
		redirectAttributes.addFlashAttribute(
				"restoredStarts",
				restoredStarts);
		
		return "redirect:/user/reservation/single";
	}
}
