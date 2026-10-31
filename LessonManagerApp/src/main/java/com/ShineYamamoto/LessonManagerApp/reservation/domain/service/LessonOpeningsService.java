package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningNotFoundException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.LessonOpeningMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonOpeningsService {

	private final LessonOpeningMapper lessonOpeningMapper;
	
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
	@Transactional
	public Long create(
			LocalDateTime startsAt,
			LocalDateTime endsAt) {
		
		checkBasicTime(startsAt, endsAt);
		
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
	@Transactional
	public void update(
			Long openingId,
			LocalDateTime startsAt,
			LocalDateTime endsAt) {
		
		checkBasicTime(startsAt, endsAt);
		
		LessonOpening original = getById(openingId);
		
		// 同じ内容なら、更新済みの状態として正常終了する
		if (original.getStartsAt().equals(startsAt) && original.getEndsAt().equals(endsAt)) {
			return;
		}
		
		LessonOpening updated = new LessonOpening();
		updated.setOpeningId(openingId);
		updated.setStartsAt(startsAt);
		updated.setEndsAt(endsAt);
		
		int updatedCount = lessonOpeningMapper.updateOne(updated);
		
		if (updatedCount != 1) {
			throw new IllegalStateException("受付可能時間を更新できませんでした。");
		}
	}
	
	/** 指定した受付可能時間を削除する */
	@Transactional
	public void delete(Long openingId) {
		
		checkId(openingId);
		
		int deletedCount = lessonOpeningMapper.deleteById(openingId);
		
		if (deletedCount == 0) {
			throw new LessonOpeningNotFoundException(openingId);
		}
		
		if (deletedCount != 1) {
			throw new IllegalStateException("受付可能時間の削除件数が想定と異なります。");
		}
	}
	
	/** IDの基本チェック */
	private void checkId(Long openingId) {
		
		if (openingId == null || openingId <= 0) {
			throw new IllegalArgumentException("受付可能時間のIDが不正です。");
		}
	}
	
	/** 保存に必要な最低限の日時チェック */
	private void checkBasicTime(
			LocalDateTime startsAt,
			LocalDateTime endsAt) {
		
		if (startsAt == null || endsAt == null) {
			
			throw new IllegalArgumentException("開始日時と終了日時は必須です。");
		}
		
		if (!startsAt.isBefore(endsAt)) {
			
			throw new IllegalArgumentException("終了日時は開始日時より後にしてください。");
		}
	}
}
