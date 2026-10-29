package com.ShineYamamoto.LessonManagerApp.reservation.repository;

import org.apache.ibatis.annotations.Mapper;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSettings;

@Mapper
public interface ReservationSettingsMapper {
	
	/** システム全体の予約設定を取得する */
	ReservationSettings findSettings();
}
