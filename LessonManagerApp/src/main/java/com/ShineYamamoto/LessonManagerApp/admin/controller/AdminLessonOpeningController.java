package com.ShineYamamoto.LessonManagerApp.admin.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.context.MessageSource;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ShineYamamoto.LessonManagerApp.admin.session.LessonOpeningTimelineEditStore;
import com.ShineYamamoto.LessonManagerApp.admin.session.LessonOpeningTimelineEditStore.Snapshot;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningBatchException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpeningBatchError;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.LessonOpeningBatchService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.LessonOpeningService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.LessonOpeningTimelineService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationSettingsService;
import com.ShineYamamoto.LessonManagerApp.reservation.form.LessonOpeningBatchForm;
import com.ShineYamamoto.LessonManagerApp.reservation.form.LessonOpeningDeleteForm;
import com.ShineYamamoto.LessonManagerApp.reservation.form.LessonOpeningForm;
import com.ShineYamamoto.LessonManagerApp.reservation.view.LessonOpeningTimelineView;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/lesson-openings")
@RequiredArgsConstructor
public class AdminLessonOpeningController {

	private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
	
	private final LessonOpeningBatchService lessonOpeningBatchService;
	private final LessonOpeningTimelineEditStore timelineEditStore;
	private final LessonOpeningService lessonOpeningService;
	private final LessonOpeningTimelineService lessonOpeningTimelineService;
	private final ReservationSettingsService reservationSettingsService;
	private final MessageSource messageSource;
	
	private static final int MAX_BATCH_ITEMS = 744;
	
	/** 
	 * バインドのルールを先に準備する処理
	 * タイムラインの情報をバインドする前に、代入可能な項目やリストの自動拡張上限を設定する */
	@InitBinder("batchForm")
	public void initBatchBinder(WebDataBinder binder) {
		
		binder.setAutoGrowCollectionLimit(MAX_BATCH_ITEMS);
		
		binder.setAllowedFields(
				"editId",
				"month",
				
				"creates[*].clientId",
				"creates[*].startsAt",
				"creates[*].endsAt",
				
				"updates[*].clientId",
				"updates[*].openingId",
				"updates[*].expectedVersion",
				"updates[*].startsAt",
				"updates[*].endsAt",
				
				"deletes[*].clientId",
				"deletes[*].openingId",
				"deletes[*].expectedVersion");
	}
	
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
	
	/** 編集画面 */
	@GetMapping("/{id}/edit")
	public String editForm(
			@PathVariable("id") Long id,
			Model model) {
		
		requirePositiveId(id);
		
		LessonOpening opening = lessonOpeningService.getById(id);
		
		LessonOpeningForm form = new LessonOpeningForm();
		form.setDate(opening.getStartsAt().toLocalDate());
		form.setStartTime(opening.getStartsAt().toLocalTime());
		form.setEndTime(opening.getEndsAt().toLocalTime());
		form.setExpectedVersion(opening.getVersion());
		
		model.addAttribute("openingForm", form);
		
		return prepareForm(id, form, model);
	}
	
	/** 更新 */
	@PostMapping("/{id}/update")
	public String update(
			@PathVariable("id") Long id,
			@Valid @ModelAttribute("openingForm") LessonOpeningForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		requirePositiveId(id);
		
		return save(id, form, bindingResult, model, redirectAttributes, locale);
	}
	
	/** 削除 */
	@PostMapping("/{id}/delete")
	public String delete(
			@PathVariable("id") Long id,
			@Valid @ModelAttribute("deleteForm") LessonOpeningDeleteForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		requirePositiveId(id);
		
		// バインド・バリデーションエラー時はリストへ戻す
		if (bindingResult.hasErrors()) {
			return redirectWithError(
					form.getDate(),
					"lessonOpening.error.invalidInput",
					redirectAttributes,
					locale);
		}
		
		try {
			lessonOpeningService.delete(id, form.getExpectedVersion());
		
		} catch (LessonOpeningCheckException e) {
			
			return redirectWithError (
					form.getDate(),
					e.getMessageKey(),
					redirectAttributes,
					locale);
			
		} catch (PessimisticLockingFailureException e) {
			
			return redirectWithError(
					form.getDate(),
					"lessonOpening.error.busy",
					redirectAttributes,
					locale);
		}
		
		redirectAttributes.addFlashAttribute(
				"message",
				messageSource.getMessage(
						"lessonOpening.success.deleted", null, locale));
		
		return redirectToList(form.getDate(), redirectAttributes);
	}
	
	/** 月単位の読み取り専用タイムライン
	 * timeline: 現在のDBから取得した最新情報
	 * originalTimeline: この編集を開始した時点の情報（セッションから取得）
	 * batchForm: 送信した追加・変更・削除の差分（POSTで差分は保存）*/
	@GetMapping("/timeline")
	public String timeline(
			@RequestParam(name = "month", required = false) String monthText,
			@RequestParam(name = "editId", required = false) String editId,
			Model model) {
		
		LocalDate today = LocalDate.now(JAPAN_ZONE);
		YearMonth month = parseTimelineMonth(monthText);
		
		Snapshot snapshot = null;
		
		// エラー後のリダイレクトなど、既存の編集を再表示する場合
		if (editId != null && !editId.isBlank()) {
			snapshot = requireEditSnapshot(editId);
			
			if (!snapshot.originalTimeline().month().equals(month.toString())) {
				
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "編集情報と対象月が一致しません。");
			}
		}
		
		// 最新の受付枠・確定予約を取得
		LessonOpeningTimelineView timeline = lessonOpeningTimelineService.getMonth(month, today);
		
		// 新しく画面を開いた場合は、編集開始時の情報を保存
		if (snapshot == null) {
			snapshot = timelineEditStore.open(timeline);
		}
		
		// Flash属性で戻ってきたフォームは上書きしない
		if (!model.containsAttribute("batchForm")) {
			LessonOpeningBatchForm form = new LessonOpeningBatchForm();
			
			form.setEditId(snapshot.editId());
			form.setMonth(snapshot.originalTimeline().month());
			
			model.addAttribute("batchForm", form);
		}
		
		model.addAttribute("timeline", timeline);
		model.addAttribute("originalTimeline", snapshot.originalTimeline());
		
		return "admin/lesson-opening/timeline";
	}
	
	@PostMapping("timeline/save")
	public String saveTimeline(
			@Valid @ModelAttribute("batchForm") LessonOpeningBatchForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		// このセッションで開始した編集か確認
		Snapshot snapshot = requireEditSnapshot(form.getEditId());
		
		// 編集開始時の月と、送信された月が一致するか
		if (!bindingResult.hasFieldErrors("month")
				&& !Objects.equals(form.getMonth(), snapshot.originalTimeline().month())) {
			
			bindingResult.rejectValue("month", "lessonOpening.error.monthMismatch");
		}
		
		// 各リストの上限とは別に、合計件数も確認
		int total = sizeOf(form.getCreates()) + sizeOf(form.getUpdates()) + sizeOf(form.getDeletes());
		
		if (total > MAX_BATCH_ITEMS) {
			bindingResult.reject("lessonOpening.error.tooManyChanges");
		}
		
		// 型変換・必須・値の範囲の変化などのエラー
		if (bindingResult.hasErrors()) {
			return redirectBatchFailure(
					form,
					bindingResult,
					toBatchErrors(bindingResult),
					snapshot,
					redirectAttributes,
					locale);
		}
		
		try {
			lessonOpeningBatchService.save(form);
			
		} catch (LessonOpeningBatchException e) {
			
			// 業務ロジックエラー：重複・予約カバー・更新競合など
			return redirectBatchFailure(
					form,
					bindingResult,
					e.getErrors(),
					snapshot,
					redirectAttributes,
					locale);
			
		} catch (PessimisticLockingFailureException e) {
			
			// DBロックの取得失敗など
			return redirectBatchFailure(
					form,
					bindingResult,
					List.of(new LessonOpeningBatchError(null, null, "lessonOpening.error.busy")),
					snapshot,
					redirectAttributes,
					locale);
					
		}
		
		// サービスが正常終了し、コミットされた後
		timelineEditStore.remove(snapshot.editId());
		
		redirectAttributes.addAttribute(
				"message",
				messageSource.getMessage(
						"lessonOpening.success.batchSaved",
						null,
						locale));
		
		redirectAttributes.addAttribute(
				"month",
				snapshot.originalTimeline().month());
		
		// editIdをつけず、新しい編集予約として表示する
		return "redirect:/admin/lesson-openings/timeline";
	}
	
	private YearMonth parseTimelineMonth(String monthText) {
		
		if (monthText == null || monthText.isBlank()) {
			return YearMonth.from(LocalDate.now(JAPAN_ZONE));
		}
		
		if (!monthText.matches("[0-9]{4}-(0[1-9]|1[0-2])")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "対象月の形式が不正です。");
		}
		
		YearMonth month = YearMonth.parse(monthText);
		
		if (month.getYear() < 1900) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "対象月が表示範囲外です。");
		}
		
		return month;
	}
	
	private Snapshot requireEditSnapshot(String editId) {
		
		Snapshot snapshot = timelineEditStore.find(editId);
		
		if (snapshot == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"編集情報が見つからないか、有効期限が切れています。"
					+ "タイムラインを開きなおしてください。");
		}
		
		return snapshot;
	}
	
	private int sizeOf(List<?> items) {
		return items == null ? 0 : items.size();
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
	
	/** 保存失敗時のリダイレクト処理 */
	private String redirectBatchFailure(
			LessonOpeningBatchForm form,
			BindingResult bindingResult,
			List<LessonOpeningBatchError> errors,
			Snapshot snapshot,
			RedirectAttributes redirectAttributes,
			Locale locale) {
		
		List<LessonOpeningBatchError> uniqueErrors = errors.stream().distinct().toList();
		
		// 入力したされた差分を保持
		redirectAttributes.addFlashAttribute("batchForm", form);
		
		// 型変換できなかった文字列なども保持
		redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "batchForm", bindingResult);
		
		// バーとの対応付けに使う情報
		redirectAttributes.addFlashAttribute("batchErrors", uniqueErrors);
		
		// 画面に表示する文章
		List<String> messages = uniqueErrors.stream()
				.map(error -> messageSource.getMessage(
						error.messageKey(),
						null,
						error.messageKey(),
						locale))
				.toList();
		
		redirectAttributes.addFlashAttribute("batchErrorMessages", messages);
		redirectAttributes.addFlashAttribute("month", snapshot.originalTimeline().month());
		redirectAttributes.addFlashAttribute("editId", snapshot.editId());
		
		return "redirect:/admin/lesson-openings/timeline";
	}
	
	private List<LessonOpeningBatchError> toBatchErrors(
			BindingResult bindingResult) {
		
		List<LessonOpeningBatchError> errors = new ArrayList<>();
		
		for (ObjectError error : bindingResult.getAllErrors()) {
			
			String clientId = null;
			String field = null;
			
			if (error instanceof FieldError fieldError) {
				field = fieldError.getField();
				clientId = findErrorClientId(bindingResult, field);
			}
			
			String code = error.getCode();
			
			String messageKey = code != null && code.startsWith("lessonOpening.")
					? code
					: "lessonOpening.error.invalidInput";
			
			errors.add(new LessonOpeningBatchError(
					clientId,
					field,
					messageKey)); 
		}
		
		return errors;
	}
	
	/** どのバーに関する入力エラーかを調べるメソッド */
	private String findErrorClientId(
			BindingResult bindingResult,
			String field) {
		
		// 例：update[0].startsAt
		int closingBracket = field.indexOf(']');
		
		// monthなど特定のバーに属さない項目
		if (closingBracket < 0) {
			return null;
		}
		
		// 例：updates[0].clientId
		String clientIdPath = field.substring(0, closingBracket + 1) + ".clientId";
		
		try {
			Object value = bindingResult.getRawFieldValue(clientIdPath);
			
			return value instanceof String clientId ? clientId : null;
		} catch (InvalidPropertyException e) {
			// 要素自体がnullなど、対象を特定できない場合
			return null;
		}
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
	
	private void requirePositiveId(Long id) {
		if (id == null || id <= 0) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"受付時間のIDが不正です。");
		}
	}
}
