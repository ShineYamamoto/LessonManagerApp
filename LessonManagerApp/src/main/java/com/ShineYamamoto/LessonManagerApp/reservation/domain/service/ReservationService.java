package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationStatus;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationLockMapper;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationCheckService reservationCheckService;
	
	private final ReservationMapper reservationMapper;
	
	private final ReservationLockMapper reservationLockMapper;
	
	/**
	 * ログインユーザー本人の予約を登録する
	 * 全件成功した場合のみ登録を確定する
	 * 排他制御により同時予約を防ぐ
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public List<ReservationSlot> reserve(
			Long userId,
			List<LocalDateTime> requestedStarts) {
		
		// 予約処理用のDBロックを取得する
		Integer lockId = reservationLockMapper.lock();
		
		if (lockId == null) {
			throw new IllegalStateException(
					"予約ロック用のデータが存在しません。");
		}
		
		// 全件をチェックし、開始・終了日時を確定する
		List<ReservationSlot> checkedSlots = reservationCheckService.check(userId, requestedStarts);
		
		// チェック済みの予約を1ずつ登録する
		for (ReservationSlot slot : checkedSlots) {
			
			Reservation reservation = new Reservation();
			
			reservation.setUserId(userId);
			reservation.setStartsAt(slot.getStartsAt());
			reservation.setEndsAt(slot.getEndsAt());
			reservation.setStatus(ReservationStatus.CONFIRMED);
			reservation.setCreatedBy(userId);
			reservation.setUpdatedBy(userId);
			
			int insertedCount = reservationMapper.insertOne(reservation);
			
			if (insertedCount != 1) {
				throw new IllegalStateException(
						"予約の登録件数が想定と異なります。");
			}
		}
		
		// このメソッドが正常終了した後にコミットされる
		// コミット時にDBロックが解放される
		return checkedSlots;
	}
	
	/**
	 * 本人の予約日時を更新する
	 * 新規予約と同じDBロックで、チェックと更新を保護する
	 */
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public ReservationSlot change(
			Long userId,
			Long reservationId,
			LocalDateTime requestedStart,
			Long expectedVersion) {
		
		// 新規予約と同じロックを取得する
		Integer lockId = reservationLockMapper.lock();
		
		if (lockId == null) {
			throw new IllegalStateException(
					"予約ロック用のデータが存在しません。");
		}
		
		// ロック取得後に最新情報でチェックする
		ReservationSlot checkedSlot = reservationCheckService.checkChange(
				userId,
				reservationId,
				requestedStart,
				expectedVersion);
		
		// チェック済みの開始・終了日時を使って更新する
		int updatedCount = reservationMapper.updateSchedule(
				reservationId,
				userId,
				checkedSlot.getStartsAt(),
				checkedSlot.getEndsAt(),
				expectedVersion);
		
		if (updatedCount != 1) {
			throw new ReservationCheckException(
					"reservation.change.error.conflict");
		}
		
		// Springがこのメソッドの正常終了後にコミットする
		return checkedSlot;
	}
}
