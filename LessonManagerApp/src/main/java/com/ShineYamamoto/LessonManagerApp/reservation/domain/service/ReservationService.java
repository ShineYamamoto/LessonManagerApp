package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationStatus;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

	private final ReservationCheckService reservationCheckService;
	
	private final ReservationMapper reservationMapper;
	
	/**
	 * ログインユーザー本人の予約を登録する
	 * 全件成功した場合のみ登録を確定する
	 */
	@Transactional
	public List<ReservationSlot> reserve(
			Long userId,
			List<LocalDateTime> requestedStarts) {
		
		
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
		
		return checkedSlots;
	}
}
