package com.ShineYamamoto.LessonManagerApp.reservation.repository;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationLockMapper {
	
	/** 予約処理用の排他ロックを取得する */
	Integer lock();
}
