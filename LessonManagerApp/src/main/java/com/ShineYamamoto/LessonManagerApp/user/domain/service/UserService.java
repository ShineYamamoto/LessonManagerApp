package com.ShineYamamoto.LessonManagerApp.user.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ShineYamamoto.LessonManagerApp.common.service.PhoneNumberService;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
	
	private final UserMapper mapper;
	private final PhoneNumberService phoneNumberService;
	
	public void signup(
			User user,
			String regionCode,
			String rawPhoneNumber) {
		
		// regionCodeと入力された電話番号からE.164形式を生成
		String e164PhoneNumber = phoneNumberService.toE164 (
				regionCode,
				rawPhoneNumber
		);
		
		user.setE164PhoneNumber(e164PhoneNumber);
		user.setRole("ROLE_GENERAL");
		
		int count = mapper.insertOne(user);
		log.info("登録件数={}件", count);
	}
	
	/** ユーザー取得 */
	public List<User> getUsers(User user) {
		return mapper.findMany(user);
	}
	
	/** ユーザーIDから1件取得 */
	public User getUserById(Long userId) {
		return mapper.findById(userId);
	}
	
	/** ユーザー更新（1件） */
	public void updateUserById(Long userId, String password, String userName) {
		int count = mapper.updateById(userId, password, userName);
		log.info("更新件数={}", count);
	}
	
	/** ユーザー削除（1件） */
	public void deleteUserById(Long userId) {
		int count = mapper.deleteById(userId);
		log.info("削除件数={}", count);
	}
}
