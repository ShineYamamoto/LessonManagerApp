package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningBatchException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpeningBatchError;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSettings;
import com.ShineYamamoto.LessonManagerApp.reservation.form.LessonOpeningBatchForm;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.LessonOpeningMapper;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationLockMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonOpeningBatchService {
	
	private final ReservationSettingsService reservationSettingsService;
	private final LessonOpeningCheckService lessonOpeningCheckService;
	private final LessonOpeningMapper lessonOpeningMapper;
	private final ReservationLockMapper reservationLockMapper;

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void save(LessonOpeningBatchForm form) {
		
		Integer lockId = reservationLockMapper.lock();
		
		if (lockId == null) {
			throw new IllegalStateException(
					"予約ロック用のデータが存在しません。");
		}
		
		YearMonth month = YearMonth.parse(form.getMonth());
		ReservationSettings settings = reservationSettingsService.getSettings();
		
		List<LessonOpeningBatchError> errors = new ArrayList<>();
		
		List<LessonOpening> currentUpdateOpenings = form.getUpdates().stream()
				.map(item -> {
					LessonOpening current = lessonOpeningMapper.findById(item.getOpeningId());
					
					// 存在・更新競合の確認
					if (current == null
							|| !Objects.equals(current.getVersion(), item.getExpectedVersion())) {
						
						errors.add(new LessonOpeningBatchError(
										item.getClientId(),
										"expectedVersion",
										"lessonOpening.error.conflict"));
						
						return null;
					}
					
					// 対象月・元の枠の編集条件を確認
					if (!checkEditableOpening(
							current,
							month,
							settings,
							item.getClientId(),
							errors)) {
						
						return null;
					}
					
					// 別の日へ移動していないか確認
					if (!checkSameDate(current, item, errors)) {
						return null;
					}
					
					return current;
				})
				.filter(Objects::nonNull)
				.toList();
		
		if (!errors.isEmpty()) {
			throw new LessonOpeningBatchException(errors);
		}
	}
	
	// 元の受付枠が編集可能か
	private boolean checkEditableOpening(
			LessonOpening current,
			YearMonth month,
			ReservationSettings settings,
			String clientId,
			List<LessonOpeningBatchError> errors) {
		
		// 必須・開始終了の順序・同日・30分単位・設定時間帯を確認
		try {
			lessonOpeningCheckService.checkPeriod(
					current.getStartsAt(),
					current.getEndsAt(),
					settings);
		} catch (LessonOpeningCheckException e) {
			
			errors.add(new LessonOpeningBatchError(
					clientId,
					"openingId",
					"lessonOpening.error.readOnly"));
		}
		
		// 元の受付枠が表示中の対象月に属しているか
		if (!YearMonth.from(current.getStartsAt()).equals(month)) {
			
			errors.add(new LessonOpeningBatchError(
					clientId,
					"openingId",
					"lessonOpening.error.outsideMonth"));
			
			return false;
		}
		
		return true;
	}
	
	// 更新前と更新後の開始・終了が、すべて同じ日付かを確認する
	private boolean checkSameDate(
			LessonOpening current,
			LessonOpeningBatchForm.UpdateItem item,
			List<LessonOpeningBatchError> errors) {
		
		if (item.getStartsAt() == null || item.getEndsAt() == null) {
			
			errors.add(new LessonOpeningBatchError(
					item.getClientId(),
					"startsAt",
					"lessonOpening.error.required"));
			
			return false;
		}
		
		LocalDate originalDate = current.getStartsAt().toLocalDate();
		
		boolean sameDate = originalDate.equals(item.getStartsAt().toLocalDate())
							&& originalDate.equals(item.getEndsAt().toLocalDate());
		
		if (!sameDate) {
			errors.add(new LessonOpeningBatchError(
					item.getClientId(),
					"startsAt",
					"lessonOpening.error.dateChange"));
			
			return false;
		}
		
		return true;
	}
}
