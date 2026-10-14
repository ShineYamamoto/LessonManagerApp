package com.ShineYamamoto.LessonManagerApp.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;

@Mapper
public interface LessonOpeningMapper {

	/** 指定期間と重なる受付時間を取得する */
	List<LessonOpening> findOverlapping(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);
}
