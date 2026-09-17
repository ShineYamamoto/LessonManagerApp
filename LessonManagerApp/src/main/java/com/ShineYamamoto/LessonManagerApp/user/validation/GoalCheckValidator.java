package com.ShineYamamoto.LessonManagerApp.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.ShineYamamoto.LessonManagerApp.goallevel.domain.service.GoalLevelService;


public class GoalCheckValidator implements ConstraintValidator<GoalCheck, Integer> {
	
	// private final UserService userService;
	private final GoalLevelService goalLevelService;
	
	public GoalCheckValidator(GoalLevelService goalLevelService) {
		this.goalLevelService = goalLevelService;
	}
	
	// 初期化処理
	@Override
	public boolean isValid(
			Integer goalLevelId,
			ConstraintValidatorContext context) {
		
		// nullは@NotNullでチェックする
		if (goalLevelId == null) {
			return true;
		}
		
		// 選択された値が目標レベル一覧（UserService.getGoalMap()に含まれているか
		return goalLevelService.isSelectable(goalLevelId);
	}
}
