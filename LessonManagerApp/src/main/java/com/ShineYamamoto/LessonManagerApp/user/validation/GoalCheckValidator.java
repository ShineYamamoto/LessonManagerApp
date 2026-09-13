package com.ShineYamamoto.LessonManagerApp.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.ShineYamamoto.LessonManagerApp.user.domain.service.UserService;


public class GoalCheckValidator implements ConstraintValidator<GoalCheck, Integer> {
	
	private final UserService userService;
	
	public GoalCheckValidator(UserService userService) {
		this.userService = userService;
	}
	
	// 初期化処理
	@Override
	public boolean isValid(
			Integer goal,
			ConstraintValidatorContext context) {
		
		// nullのチェックは@Nullでチェックする
		if (goal == null) {
			return true;
		}
		
		// 選択された値が目標レベル一覧（UserService.getGoalMap()に含まれているか
		return userService.getGoalMap().containsValue(goal);
	}
}
