package com.ShineYamamoto.LessonManagerApp.user.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;

@Mapper
public interface UserMapper {
	
	/** ユーザー登録 */
	public int insertOne(User user);
	
	/** ユーザー件数取得 */
	public int count(User user);
	
	/** ユーザー取得（複数） */
	public List<User> findMany(
			@Param("user") User user,
			@Param("pageable") Pageable pageable);
	
	/** ユーザーIDから1件取得 */
	public User findById(Long userId);
	
	/** ユーザー更新(1件) */
	public int updateById(
			@Param("userId") Long userId,
			@Param("password") String password,
			@Param("userName")  String userName);
	
	/** ユーザー削除（1件） */
	public int deleteById(Long userId);
}
