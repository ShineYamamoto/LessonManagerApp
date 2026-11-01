package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningNotFoundException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.LessonOpeningMapper;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationLockMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonOpeningService {

	private final LessonOpeningMapper lessonOpeningMapper;
	private final LessonOpeningCheckService lessonOpeningCheckService;
	private final ReservationLockMapper reservationLockMapper;
	
	
	/** IDを指定して受付時間を取得する */
	public LessonOpening getById(Long openingId) {
		
		checkId(openingId);
		
		LessonOpening opening = lessonOpeningMapper.findById(openingId);
		
		if (opening == null) {
			throw new LessonOpeningNotFoundException(openingId);
		}
		
		return opening;
	}
	
	/** 受付可能時間を登録し、採番されたIDを返す */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Long create(
			LocalDateTime startsAt,
			LocalDateTime endsAt) {
		
		lock();
		
		lessonOpeningCheckService.checkForSave(startsAt, endsAt, null);
		
		LessonOpening opening = new LessonOpening();
		opening.setStartsAt(startsAt);
		opening.setEndsAt(endsAt);
		
		int insertedCount = lessonOpeningMapper.insertOne(opening);
		
		if (insertedCount != 1 || opening.getOpeningId() == null) {
			
			throw new IllegalStateException("受付可能時間を正常に登録できませんでした");
		}
		
		return opening.getOpeningId();
	}
	
	/** 指定した受付可能時間の開始・終了日時を更新する */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void update(
			Long openingId,
			LocalDateTime startsAt,
			LocalDateTime endsAt,
			Long expectedVersion) {
		
		checkId(openingId);
		
		lock();
		
		LessonOpening original = getById(openingId);
		
		// 古い画面から送信されたリクエストかチェック
		checkVersion(original, expectedVersion);
		
		lessonOpeningCheckService.checkForSave(startsAt, endsAt, openingId);
		
		// 同じ内容なら、更新済みの状態として正常終了する
		if (original.getStartsAt().equals(startsAt) && original.getEndsAt().equals(endsAt)) {
			return;
		}
		
		LessonOpening updated = new LessonOpening();
		updated.setOpeningId(openingId);
		updated.setStartsAt(startsAt);
		updated.setEndsAt(endsAt);
		updated.setVersion(expectedVersion);
		
		// 更新しても既存予約を受付時間で覆えるか
		lessonOpeningCheckService.checkReservationsPreserved(original, updated);
		
		int updatedCount = lessonOpeningMapper.updateOne(updated);
		
		if (updatedCount != 1) {
			throw new IllegalStateException("lessonOpening.error.conflict");
		}
	}
	
	/** 指定した受付可能時間を削除する */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void delete(
			Long openingId,
			Long expectedVersion) {
		
		checkId(openingId);
		
		lock();
		
		LessonOpening original = getById(openingId);
		
		checkVersion(original, expectedVersion);
		
		// 更新しても受付時間を削除できるか
		lessonOpeningCheckService.checkReservationsPreserved(original, null);
		
		int deletedCount = lessonOpeningMapper.deleteById(openingId, expectedVersion);
		
		if (deletedCount != 1) {
			throw new IllegalStateException("lessonOpening.error.conflict");
		}
	}
	
	/** 指定日の受付時間を開始順で取得する */
	public List<LessonOpening> getByDate(LocalDate date) {
		
		if (date == null) {
			throw new IllegalArgumentException("日付は必須です。");
		}
		
		return lessonOpeningMapper.findOverlapping(
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());
	}
	
	/** 予約処理と共通のDBロックを取得する */
	private void lock() {
		
		Integer lockId = reservationLockMapper.lock();
		
		if (lockId == null) {
			throw new IllegalStateException("予約ロック用のデータが存在しません。");
		}
	}
	
	/** IDの基本チェック */
	private void checkId(Long openingId) {
		
		if (openingId == null || openingId <= 0) {
			throw new IllegalArgumentException("受付可能時間のIDが不正です。");
		}
	}
	
	private void checkVersion(
			LessonOpening original,
			Long expectedVersion) {
		
		if (expectedVersion == null
				|| expectedVersion < 0
				|| !expectedVersion.equals(original.getVersion())) {
			
			throw new LessonOpeningCheckException("lessonOpening.error.conflict");
		}
	}
}
