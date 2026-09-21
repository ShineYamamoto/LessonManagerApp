package com.ShineYamamoto.LessonManagerApp.user.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;

@Mapper
public interface UserMapper {
	
	/** ユーザー登録 */
	public int insertOne(User user);
	
	/** ユーザー取得（複数） */
	public List<User> findMany(User user);
	
	/** ユーザーIDから1件取得 */
	public User findById(Long userId);
	
	/** ユーザー更新(1件) */
	public int updateById(Long userId, String password, String userName);
	
	/** ユーザー削除（1件） */
	public int deleteById(Long userId);
}
