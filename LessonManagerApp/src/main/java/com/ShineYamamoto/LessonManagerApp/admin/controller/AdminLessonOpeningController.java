package com.ShineYamamoto.LessonManagerApp.admin.controller;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
