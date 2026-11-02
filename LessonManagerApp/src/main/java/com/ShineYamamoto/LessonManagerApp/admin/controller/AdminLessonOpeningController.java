package com.ShineYamamoto.LessonManagerApp.admin.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.LessonOpeningService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationSettingsService;
import com.ShineYamamoto.LessonManagerApp.reservation.form.LessonOpeningForm;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/lesson-openings")
@RequiredArgsConstructor
public class AdminLessonOpeningController {

	private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
	
	private final LessonOpeningService lessonOpeningService;
	private final ReservationSettingsService reservationSettingsService;
	private final MessageSource messageSource;
	
	/** 日付別一覧 */
	@GetMapping
	public String list(
			@RequestParam(name = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			Model model) {
		
		
		LocalDate selectedDate = dateOrToday(date);
		
		model.addAttribute("date", selectedDate);
		model.addAttribute("previousDate", selectedDate.minusDays(1));
		model.addAttribute("nextDate", selectedDate.plusDays(1));
		model.addAttribute("openings", lessonOpeningService.getByDate(selectedDate));
		model.addAttribute("settings", reservationSettingsService.getSettings());
		
		return "admin/lesson-opening/list";
	}
	
	/** 新規登録画面 */
	@GetMapping("/new")
	public String newForm(
			@RequestParam(name = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			Model model) {
		
		LessonOpeningForm form = new LessonOpeningForm();
		form.setDate(dateOrToday(date));
		
		model.addAttribute("openingForm", form);
		
		return prepareForm(null, form, model);
	}
	
	@PostMapping("/create")
	public String create(
			@Valid @ModelAttribute("openingForm") LessonOpeningForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		return save(null, form, bindingResult, model, redirectAttributes, locale);
	}
	
	/** 登録・更新の共通処理 */
	private String save(
			Long id,
			LessonOpeningForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		// 編集画面の更新番号が欠けている・不正な場合は開きなおす
		if (id != null
				&& (bindingResult.hasFieldErrors("expectedVersion")
				|| form.getExpectedVersion() == null)) {
			
			return redirectWithError(
					form.getDate(),
					"lessonOpening.error.conflict",
					redirectAttributes,
					locale);
		}
		
		// 日付・時刻の必須エラーや形式エラー
		if (bindingResult.hasErrors()) {
			return prepareForm(id, form, model);
		}
		
		LocalDateTime startsAt = form.getDate().atTime(form.getStartTime());
		LocalDateTime endsAt = form.getDate().atTime(form.getEndTime());
		
		try {
			if (id == null) {
				lessonOpeningService.create(startsAt, endsAt);
			} else {
				lessonOpeningService.update(id, startsAt, endsAt, form.getExpectedVersion());
			}
		} catch (LessonOpeningCheckException e) {
			
			// 古い画面の内容を、最新の更新番号で送り直させない
			if ("lesson.Opening.error.conflict".equals(e.getMessageKey())) {
				return redirectWithError(
						form.getDate(),
						e.getMessageKey(),
						redirectAttributes,
						locale);
			}
			
			// 入力内容を保持したまま、フォームに業務エラーを表示
			bindingResult.reject(e.getMessageKey());
			return prepareForm(id, form, model);
		
		} catch (PessimisticLockingFailureException e) {
			
			// ロック取得失敗などが PessimisticLockingFailureException として通知された場合は、一覧へ戻して再操作を案内する
			bindingResult.reject("lessonOpening.error.busy");
			return prepareForm(id, form, model);
		}
		
		redirectAttributes.addFlashAttribute(
				"message",
				messageSource.getMessage(
						id == null
							? "lessonOpening.success.created"
							: "lessonOpening.success.updated",
						null,
						locale));
		
		return redirectToList(form.getDate(), redirectAttributes);
	}
	
	private String redirectWithError(
			LocalDate date,
			String messageKey,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		redirectAttributes.addFlashAttribute(
				"errorMessage",
				messageSource.getMessage(messageKey, null, locale));
		
		return redirectToList(date, redirectAttributes);
	}
	
	private String redirectToList(
			LocalDate date,
			RedirectAttributes redirectAttributes) {
		
		redirectAttributes.addAttribute(
				"date",
				dateOrToday(date).toString());
		
		return "redirect:/admin/lesson-openings";
	}
	
	/** フォーム表示に必要な情報を設定する */
	private String prepareForm(
			Long id,
			LessonOpeningForm form,
			Model model) {
		
		model.addAttribute("editing", id != null);
		model.addAttribute("openingId", id);
		model.addAttribute("backDate", dateOrToday(form.getDate()));
		model.addAttribute("settings", reservationSettingsService.getSettings());
		
		return "admin/lesson-opening/form";
	}
	
	private LocalDate dateOrToday(LocalDate date) {
		return date != null ? date : LocalDate.now(JAPAN_ZONE);
	}
	
}
