package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.LessonOpeningMapper;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationMapper;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationDataService {

	private final UserMapper userMapper;
	private final LessonOpeningMapper lessonOpeningMapper;
	private final ReservationMapper reservationMapper;
	
	/** ユーザーの授業時間を取得する */
	public int getLessonMinutes(Long userId) {
		
		Integer minutes = userMapper.findLessonMinutesByUserId(userId);
		
		if (minutes == null) {
			throw new IllegalStateException(
					"対象ユーザーが見つかりません。");
		}
		
		if (minutes <= 0 || minutes % 30 != 0) {
			throw new IllegalStateException(
					"ユーザーの授業時間設定が不正です。");
		}
		
		return minutes;
	}
	
	/** 指定日の受付可能時間を取得する */
	public List<LessonOpening> getOpening(LocalDate date) {
		
		// 指定日の0時を作成
		LocalDateTime from = date.atStartOfDay();
		LocalDateTime to = date.plusDays(1).atStartOfDay();
		
		return lessonOpeningMapper.findOverlapping(from, to);
	}
	
	/** 指定日と重なる確定予約を取得する */
	public List<Reservation> getConfirmedReservation(LocalDate date) {
		
		LocalDateTime from = date.atStartOfDay();
		LocalDateTime to = date.plusDays(1).atStartOfDay();
		
		return reservationMapper.findConfirmedOverlapping(from, to);
	}
	
	/** 本人の、これから始まる確定予約一覧を取得する */
	public List<Reservation> getUpcomingReservations(
			Long userId,
			LocalDateTime now,
			int limit) {
		
		return reservationMapper.findUpcomingByUserId(
				userId,
				now,
				limit);
	}
	
	/** 本人の、これから始まる確定予約を1件取得する */
	public Optional<Reservation> getUpcomingReservation(
			Long userId,
			Long reservationId,
			LocalDateTime now) {
		
		Reservation reservation = reservationMapper.findUpcomingByIdAndUserId(
				reservationId,
				userId,
				now);
		
		// 見つからなかった場合：空のOptionalを返す
		return Optional.ofNullable(reservation);
	}
}
