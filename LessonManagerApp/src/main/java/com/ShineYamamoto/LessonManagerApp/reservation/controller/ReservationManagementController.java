package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import static com.ShineYamamoto.LessonManagerApp.reservation.domain.policy.ReservationPolicy.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationDataService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationService;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationChangeForm;
import com.ShineYamamoto.LessonManagerApp.reservation.view.ReservationView;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/reservation")
@RequiredArgsConstructor
public class ReservationManagementController {
	
	private final ReservationDataService reservationDataService;
	
	private final ReservationService reservationService;
	
	private final MessageSource messageSource;
	
	private static final DateTimeFormatter CHANGE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm");

	private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
	
	private static final int MAX_DISPLAY_COUNT = 50;
	
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
		model.addAttribute("reservationVersion", reservation.getVersion());
		
		// 実際に予約された予約の授業時間を維持する
		long lessonMinutes = Duration.between(
				reservation.getStartsAt(),
				reservation.getEndsAt()).toMinutes();
		
		model.addAttribute("lessonMinutes", lessonMinutes);
		
		return "reservation/reservation-edit";
	}
	
	/**
	 * 変更内容を受信してチェック後に予約を更新する
	 */
	@PostMapping("/{reservationId}/edit")
	public String postReservationEdit(
			@PathVariable("reservationId") Long reservationId,
			@Valid @ModelAttribute("changeForm") ReservationChangeForm form,
			BindingResult bindingResult,
			@AuthenticationPrincipal LoginUser loginUser,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		String editRedirect = "redirect:/user/reservation/" + reservationId + "/edit";
		
		// 未入力や日時形式の不正
		if (bindingResult.hasErrors()) {
			
			redirectAttributes.addFlashAttribute(
					"reservationError",
					messageSource.getMessage(
							"reservation.error.formInvalid",
							null,
							locale));
			
			return editRedirect;
		}
		
		LocalDateTime requestedStart = form.getStartsAt();
		
		try {
			
			ReservationSlot changedSlot = reservationService.change(
					loginUser.getUserId(),
					reservationId,
					requestedStart,
					form.getExpectedVersion());
			
			// Serviceのトランザクションがコミットされた後に到達する
			String notice = messageSource.getMessage(
					"reservation.change.completed",
					new Object[] {
							changedSlot.getStartsAt().format(CHANGE_DATE_FORMAT),
							changedSlot.getEndsAt().format(CHANGE_DATE_FORMAT)
					},
					locale);
			
			redirectAttributes.addFlashAttribute("reservationNotice", notice);
			
			return "redirect:/user/reservation/list";
			
		} catch (ReservationCheckException e) {
			
			String errorMessage = messageSource.getMessage(
					e.getMessageKey(),
					new Object[] {
							requestedStart.format(CHANGE_DATE_FORMAT),
							CHANGE_LEAD_HOURS
					},
					locale);
			
			redirectAttributes.addFlashAttribute("reservationError", errorMessage);
			
			// 古い画面の場合は一覧から最新の予約を確認してもらう
			if ("reservation.change.error.conflict".equals(e.getMessageKey())) {
				return "redirect:/user/reservation/list";
			}
			
			// 日時の条件違反なら、選択内容を残して編集画面へ戻す
			redirectAttributes.addFlashAttribute(
					"restoredStarts",
					requestedStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			
			return editRedirect;
			
		} catch (PessimisticLockingFailureException e) {
			
			// ロック取得失敗などが PessimisticLockingFailureException として通知された場合は、一覧へ戻して再操作を案内する
			redirectAttributes.addFlashAttribute(
					"reservationError",
					messageSource.getMessage(
							"reservation.change.error.busy",
							null,
							locale));
			
			return "redirect:/user/reservation/list";
		}
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
