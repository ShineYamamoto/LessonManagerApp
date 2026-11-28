package com.ShineYamamoto.LessonManagerApp.user.domain.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.common.service.PhoneNumberService;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
	
	private final UserMapper mapper;
	private final PhoneNumberService phoneNumberService;
	private final PasswordEncoder encoder;
	
	public void signup(
			User user,
			String regionCode,
			String rawPhoneNumber) {
		
		// regionCodeと入力された電話番号からE.164形式を生成
		String e164PhoneNumber = phoneNumberService.toE164 (
				regionCode,
				rawPhoneNumber
		);
		
		// パスワードのハッシュ化
		String hashPassword = user.getPassword();
		user.setPassword(encoder.encode(hashPassword));
		
		user.setE164PhoneNumber(e164PhoneNumber);
		user.setRole("ROLE_GENERAL");
		
		int count = mapper.insertOne(user);
		log.info("登録件数={}件", count);
	}
	
	/** ユーザー取得 */
	@PreAuthorize("hasRole('ADMIN')")
	public Page<User> getUsers(User user, Pageable pageable) {
		// ユーザー一覧取得
		List<User> userList = mapper.findMany(user, pageable);
		// ユーザー一覧の件数取得
		int count = mapper.count(user);
		// Pageのインスタンス生成
		return new PageImpl<User>(userList, pageable, count);
	}
	
	/** ユーザーIDから1件取得 */
	public User getUserById(Long userId) {
		return mapper.findById(userId);
	}
	
	/** ユーザー名更新 */
	public String updateUserName(Long userId, String userName) {
		if (userId == null || userName == null) {
			throw new IllegalArgumentException("更新するユーザーまたは名前が不正です");
		}
		String normalized = userName.strip();
		if (normalized.isBlank() || normalized.length() > 50) {
			throw new IllegalArgumentException("ユーザー名は1～50文字で入力してください");
		}
		if (mapper.updateUserName(userId, normalized) != 1) {
			throw new IllegalStateException("ユーザー名を更新できませんでした。");
		}
		return normalized;
	}
	
	/** ユーザーの電話番号を変更する */
	public PhoneUpdateResult updatePhoneNumber(
			Long userId,
			String oldPhoneNumber,
			String regionCode,
			String rawPhoneNumber,
			String currentPassword) {
		
		// 対応する国・地域か確認
		if (!"JP".equals(regionCode) && !"CN".equals(regionCode)) {
			return PhoneUpdateResult.INVALID_PHONE;
		}
		
		// E.164形式に変換
		String newPhoneNumber;
		
		try {
			
			newPhoneNumber = phoneNumberService.toE164(regionCode, rawPhoneNumber);
			
		} catch (IllegalArgumentException e) {
			
			return PhoneUpdateResult.INVALID_PHONE;
		}
		
		// 選択された国と一致する電話番号か確認
		String countryCode = "JP".equals(regionCode) ? "+81" : "+86";
		
		if (!newPhoneNumber.startsWith(countryCode)) {
			
			return PhoneUpdateResult.INVALID_PHONE;
		}
		
		// 現在のユーザー情報を取得
		User user = mapper.findByE164PhoneNumber(oldPhoneNumber);
		
		// ログイン情報とDBの情報が一致するか確認
		if (user == null || !userId.equals(user.getUserId())) {
			return PhoneUpdateResult.STALE_LOGIN;
		}
		
		// 現在のパスワードを照合
		if (!encoder.matches(currentPassword, user.getPassword())) {
			return PhoneUpdateResult.INVALID_PASSWORD;
		}
		
		// 同じ電話番号か確認
		if (newPhoneNumber.equals(user.getE164PhoneNumber())) {
			return PhoneUpdateResult.SAME_PHONE;
		}
		
		// 電話番号を更新
		int count = mapper.updatePhoneNumber(
				userId,
				oldPhoneNumber,
				newPhoneNumber,
				user.getPassword());
		
		if (count != 1) {
			return PhoneUpdateResult.STALE_LOGIN;
		}
		
		return PhoneUpdateResult.SUCCESS;
	}
	
	/** ユーザーのパスワードを変更する */
	public PasswordUpdateResult updatePassword(
			Long userId,
			String e164PhoneNumber,
			String currentPassword,
			String newPassword) {
		
		// 新しいパスワードの妥当性チェック
		if (newPassword == null || !newPassword.matches("^[\\x21-\\x7E]{4, 100}$")) {
			return PasswordUpdateResult.INVALID_NEW_PASSWORD;
		}
		
		// 現在のユーザー情報の取得
		User user = mapper.findByE164PhoneNumber(e164PhoneNumber);
		
		// ログイン情報とDBの情報が一致するか確認
		if (user == null || !userId.equals(user.getUserId())) {
			return PasswordUpdateResult.STALE_LOGIN;
		}
		
		// 現在のパスワードを照合
		if (currentPassword == null
				|| !encoder.matches(currentPassword, user.getPassword())) {
			
			return PasswordUpdateResult.INVALID_CURRENT_PASSWORD;
		}
		
		// 現在と同じパスワードか確認
		if (encoder.matches(newPassword, user.getPassword())) {
			return PasswordUpdateResult.SAME_PASSWORD;
		}
		
		// 新しいパスワードをハッシュ化
		String newPasswordHash = encoder.encode(newPassword);
		
		// パスワードを更新
		int count = mapper.updatePassword(userId, e164PhoneNumber, newPasswordHash, user.getPassword());
		
		// 更新結果を確認
		if (count != 1) {
			return PasswordUpdateResult.STALE_LOGIN;
		}
		
		return PasswordUpdateResult.SUCCESS;
	}
	
	/** ユーザー更新（1件） */
	public void updateUserById(Long userId, String password, String userName) {
		
		// パスワードのハッシュ化
		String hashPassword = encoder.encode(password);
		int count = mapper.updateById(userId, hashPassword, userName);
		log.info("更新件数={}", count);
	}
	
	/** ユーザー削除（1件） */
	public void deleteUserById(Long userId) {
		int count = mapper.deleteById(userId);
		log.info("削除件数={}", count);
	}
	
	/**　電話番号更新結果を定義 */
	public enum PhoneUpdateResult {
		
		SUCCESS,
		
		INVALID_PHONE,
		
		INVALID_PASSWORD,
		
		SAME_PHONE,
		
		STALE_LOGIN
	}
	
	/** パスワード更新結果を定義 */
	public enum PasswordUpdateResult {
		
		SUCCESS,
		
		INVALID_CURRENT_PASSWORD,
		
		INVALID_NEW_PASSWORD,
		
		SAME_PASSWORD,
		
		STALE_LOGIN
	}
}
