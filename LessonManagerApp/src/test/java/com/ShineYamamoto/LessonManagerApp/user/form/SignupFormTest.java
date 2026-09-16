package com.ShineYamamoto.LessonManagerApp.user.form;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ShineYamamoto.LessonManagerApp.user.validation.GoalCheck;

@SpringBootTest
public class SignupFormTest {
	
	@Autowired
	private Validator validator;
	
	@Test
	void 選択された目標レベルが一覧にない場合はGoakCheckでエラーになる() {
		
		// 他の項目には正常な値を設定
		SignupForm form = new SignupForm();
		form.setRegionCode("+86");
		form.setPhoneNumber("13812345678");
		form.setUserName("山田太郎");
		form.setPassword("test1234");
		
		// 一覧にない目標レベルを設置
		form.setGoal(-99);
		
		// バリデーションを実行
		// validator.validate(form)が、SignupFormに付いているすべてのバリデーションを実行
		// 検出されたエラーはConstraintViolation<SignupForm>として返される
		// 返されたエラーをSetにまとめる
		Set<ConstraintViolation<SignupForm>> violations = validator.validate(form);
		
		// Goal項目にGoalCheckエラーがあるか確認
		// stream()を使って、violationsに格納されているエラーを1件ずつ調べる
		// anyMatch()は、条件に該当するものが1件でも存在すればtrueを返すメソッド
		// getPropertyPath()は、どのフィールドでエラーが発生したかを取得する
		// getConstraintDescriptor()は、エラーになったバリデーションの詳細情報を取得する
		boolean hasGoalCheckError = violations.stream()
					.anyMatch(violation ->
						violation.getPropertyPath().toString().equals("goal")
						&& violation.getConstraintDescriptor()
								.getAnnotation()
								.annotationType()
								.equals(GoalCheck.class)
					);
		
		// 確認
		assertTrue(hasGoalCheckError);
		
	}
}
