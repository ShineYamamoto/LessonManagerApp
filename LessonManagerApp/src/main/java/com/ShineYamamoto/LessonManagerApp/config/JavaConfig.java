package com.ShineYamamoto.LessonManagerApp.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ShineYamamoto.LessonManagerApp.admin.user.form.AdminUserDetailForm;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.form.UserDetailForm;

@Configuration
public class JavaConfig {
	
	
	/* 
	 * ユーザー詳細を表示するさい、mapperからUserクラスへ値を登録したものをUserDetailFormへマッピングする。
	 * しかし、User型をBean登録したさい、自動的に
	 * User.getGoalLevelId()
	 * User.getGoalLevel().getId()
	 * のふたつが生成されてしまい、どちらのフィールドをUserDetailForm.setGoalLevelId()
	 * へ登録すればよいか判定できなくなる問題が生じる。
	 * 対策として以下のModelMapperのBean登録の際にUser.getGoalLevelId()をセットすることを明示しておく。*/
	@Bean
	ModelMapper modelMaper() {
		
		ModelMapper modelMapper = new ModelMapper();
		
		// 管理者用ユーザー詳細画面
		modelMapper.addMappings(
				
				// UserからAdminUserDetailFormへ値を移す
				new PropertyMap<User, AdminUserDetailForm>() {
					
					// AdminUserDetailFormのgoalLevelIdには、User.goalLevelIdを入れる
					@Override
					protected void configure() {
						map().setGoalLevelId(source.getGoalLevelId());
					}
				}
		);
		
		// 一般ユーザー用ユーザー詳細画面
		modelMapper.addMappings(
				
				// UserからUserDetailFormへ値を移す
				new PropertyMap<User, UserDetailForm>() {
					
					// UserDetailFormのgoalLevelIdには、User.goalLevelIdを入れる
					@Override
					protected void configure() {
						map().setGoalLevelId(source.getGoalLevelId());
					}
				}
		);
		
		
		return modelMapper;
	}
}
