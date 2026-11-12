package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSettings;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.LessonOpeningMapper;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonOpeningCheckService {

	private static final int STEP_MINUTES = 30;
	
	private final ReservationSettingsService reservationSettingsService;
	private final LessonOpeningMapper lessonOpeningMapper;
	private final ReservationMapper reservationMapper;
	
	/**
	 * 登録・更新する受付時間を検証する
	 * 
	 * excludedOpeningId
	 * 新規登録ではnull
	 * 更新では更新対象の受付時間ID
	 */
	public void checkForSave(
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			Long excludedOpeningId) {
		
		// 時間帯の制限はDBから取得する
		ReservationSettings settings = reservationSettingsService.getSettings();
		
		checkPeriod(startsAt, endsAt, settings);
		
		// 今回の時間帯と重なる受付時間を取得する
		List<LessonOpening> overlapping = lessonOpeningMapper.findOverlapping(startsAt, endsAt);
		
		// 更新の場合、自分自身は重複判定から除外する
		boolean duplicated = overlapping.stream()
				.anyMatch(opening ->
						!Objects.equals(opening.getOpeningId(), excludedOpeningId));
		
		// 自分自身以外で重複している受付時間があればエラーを返す
		if (duplicated) {
			throw new LessonOpeningCheckException("lessonOpening.error.overlap");
		}
	}
	
	public void checkPeriod(
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			ReservationSettings settings) {
		
		// 開始・終了は必須
		if (startsAt == null || endsAt == null) {
			throw new LessonOpeningCheckException("lessonOpening.error.required");
		}
		
		// 開始より終了が後か
		if (!startsAt.isBefore(endsAt)) {
			throw new LessonOpeningCheckException("lessonOpening.error.timeOrder");
		}
		
		// 開始と終了は同じ日付か
		if (!startsAt.toLocalDate().equals(endsAt.toLocalDate())) {
			throw new LessonOpeningCheckException("lessonOpening.error.sameDay");
		}
		
		// 開始と終了は30分単位か
		if (!isStepAligned(startsAt) || !isStepAligned(endsAt)) {
			throw new LessonOpeningCheckException("lessonOpening.error.invalidStep");
		}
		
		// 現在の設定時間帯に収まるか
		if (startsAt.toLocalTime().isBefore(settings.getSelectableStartTime())
				|| endsAt.toLocalTime().isAfter(settings.getSelectableEndTime())) {
			
			throw new LessonOpeningCheckException("lessonOpening.error.outsideHours");
		}
	}
	
	/**
	 * 更新・削除後も、既存の確定予約を受付時間で覆えるか確認する
	 * 
	 * original: 変更前の受付時間
	 * replacement: 変更後の受付時間。削除の場合はnull
	 */
	public void checkReservationsPreserved(
			LessonOpening original,
			LessonOpening replacement) {
		
		// 今回の変更によって影響を受ける可能性がある確定予約
		List<Reservation> reservations = reservationMapper.findConfirmedOverlapping(
				original.getStartsAt(),
				original.getEndsAt());
		
		for (Reservation reservation : reservations) {
			
			// この予約全体と重なる受付時間を取得する
			List<LessonOpening> afterOpenings = new ArrayList<>(
					lessonOpeningMapper.findOverlapping(
							reservation.getStartsAt(),
							reservation.getEndsAt()));
			
			// 変更前の受付時間を除外する
			afterOpenings.removeIf(opening -> 
					Objects.equals(
							opening.getOpeningId(),
							original.getOpeningId()));
			
			// 更新の場合は、変更後の受付時間を加える
			if (replacement != null) {
				afterOpenings.add(replacement);
			}
			
			// 変更後の状態を使い、予約全体を覆えるか判定する
			if (!isCovered(
					reservation.getStartsAt(),
					reservation.getEndsAt(),
					afterOpenings)) {
				
				throw new LessonOpeningCheckException("lessonOpening.error.reservationExists");
			}
		}
	}
	
	
	/** 秒・小数秒を含まない30分単位か */
	private boolean isStepAligned(LocalDateTime dateTime) {
		
		return dateTime.getMinute() % STEP_MINUTES == 0
				&& dateTime.getSecond() == 0
				&& dateTime.getNano() == 0;
	}
	
	/** 複数の受付時間を組み合わせて、授業全体を覆えるか */
	public boolean isCovered(
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			List<LessonOpening> openings) {
		
		// 受付時間を開始時刻順に並べる
		List<LessonOpening> sorted = openings.stream()
				.sorted(Comparator.comparing(LessonOpening::getStartsAt))
				.toList();
		
		// 予約の開始から、この時刻まで隙間なく受付可能だと確認できた位置
		LocalDateTime coveredUntil = startsAt;
		
		for (LessonOpening opening : sorted) {
			
			// 確認済みの位置までに終わっている受付時間は飛ばす
			if (!opening.getEndsAt().isAfter(coveredUntil)) {
				continue;
			}
			
			// 次の授業時間までに隙間がある場合は失敗
			if (opening.getStartsAt().isAfter(coveredUntil)) {
				return false;
			}
			
			// 隙間がなければ、確認済みの位置を進める
			coveredUntil = opening.getEndsAt();
			
			// 授業終了まで受付時間が連続している
			if (!coveredUntil.isBefore(endsAt)) {
				return true;
			}
		}
		
		return false;
	}
}
