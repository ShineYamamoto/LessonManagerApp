package com.ShineYamamoto.LessonManagerApp.user.domain.model;

import lombok.Data;

@Data
public class GoalLevel {

	private Integer id;
	private String levelCode;
	private Integer displayOrder;
	private Boolean enabled;
}
