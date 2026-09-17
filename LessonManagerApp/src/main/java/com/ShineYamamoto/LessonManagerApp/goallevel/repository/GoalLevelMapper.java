package com.ShineYamamoto.LessonManagerApp.goallevel.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ShineYamamoto.LessonManagerApp.goallevel.domain.model.GoalLevel;

@Mapper
public interface GoalLevelMapper {

	/** 選択可能な目標レベルを取得 */
	List<GoalLevel> findAllEnabled();
	
	/** 指定したIDが選択可能か確認 */
	int countEnabledById(Integer id);
}
