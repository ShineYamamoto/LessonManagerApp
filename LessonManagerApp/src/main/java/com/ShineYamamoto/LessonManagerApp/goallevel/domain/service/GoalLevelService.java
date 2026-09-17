package com.ShineYamamoto.LessonManagerApp.goallevel.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ShineYamamoto.LessonManagerApp.goallevel.domain.model.GoalLevel;
import com.ShineYamamoto.LessonManagerApp.goallevel.repository.GoalLevelMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoalLevelService {
	
	private final GoalLevelMapper mapper;

	public List<GoalLevel> getSelectableGoalLevels () {
		return mapper.findAllEnabled();
	}
	
	public boolean isSelectable(Integer goalLevelId) {
		if (goalLevelId == null) {
			return false;
		}
		
		return mapper.countEnabledById(goalLevelId) > 0;
	}
}
