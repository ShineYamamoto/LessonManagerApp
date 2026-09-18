package com.ShineYamamoto.LessonManagerApp.user.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;

@Mapper
public interface UserMapper {
	
	/** ユーザー登録 */
	public int insertOne(User user);
	
	/** ユーザー取得（複数） */
	public List<User> findMany();
}
