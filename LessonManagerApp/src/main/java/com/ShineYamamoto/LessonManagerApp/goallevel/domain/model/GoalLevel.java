package com.ShineYamamoto.LessonManagerApp.goallevel.domain.model;

import lombok.Data;

@Data
public class GoalLevel {
	
	private Integer id;
	
	private String levelCode;
	
	private Integer displayOrder;
	
	private Boolean enabled;
}
