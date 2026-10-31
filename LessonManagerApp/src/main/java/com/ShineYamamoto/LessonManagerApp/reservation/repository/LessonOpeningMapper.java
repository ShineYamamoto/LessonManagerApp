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
	
	/** IDを指定して1件取得する */
	LessonOpening findById(
			@Param("openingId") Long openingId);
	
	/** 受付可能時間を1件登録する */
	int insertOne(LessonOpening opening);
	
	/** 指定した受付可能時間の開始・終了日時を更新する */
	int updateOne(LessonOpening opening);
	
	/** 指定した受付可能時間を削除する */
	int deleteById(
			@Param("openingId") Long openingId);

}
