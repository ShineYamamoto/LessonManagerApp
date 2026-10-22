package com.ShineYamamoto.LessonManagerApp.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;

@Mapper
public interface ReservationMapper {

	/** 指定時間と重なる確定予約を取得する */
	List<Reservation> findConfirmedOverlapping(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);
	
	/** 予約を1件登録する */
	int insertOne(Reservation reservation);
	
	/** 本人の、これから始まる確定予約を取得する */
	List<Reservation> findUpcomingByUserId(
			@Param("userId") Long userId,
			@Param("now") LocalDateTime now,
			@Param("limit") int limit);
	
	/** 本人の、これから始まる確定予約を予約IDで1件取得する */
	Reservation findUpcomingByIdAndUserId(
			@Param("reservationId") Long reservationId,
			@Param("userId") Long userId,
			@Param("now") LocalDateTime now);
}
