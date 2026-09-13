package com.ShineYamamoto.LessonManagerApp.user.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = { GoalCheckValidator.class })
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GoalCheck {
	
	/** デフォルトメッセージ */
	String message() default "{GoalCheck}";
	
	/** グループ */
	Class<?>[] groups() default {};
	
	/** ペイロード */
	Class<? extends Payload>[] payload() default {};
}
