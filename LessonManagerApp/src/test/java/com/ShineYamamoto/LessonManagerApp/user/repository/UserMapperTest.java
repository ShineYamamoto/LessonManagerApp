package com.ShineYamamoto.LessonManagerApp.user.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.user.domain.model.User;

@SpringBootTest(properties = {
		"spring.sql.init.model=always",
		"spring.sql.init.schema-locations=classpath:schema.sql",
		// data.sqlに依存せず、各テストのみで必要なデータを用意する
		"spring.sql.init.data-locations=optional:classpath:/user-mapper-test-no-data.sql"
})
@AutoConfigureTestDatabase(replace = Replace.ANY)
@Transactional
public class UserMapperTest {

	@Autowired
	private UserMapper mapper;
	
	@Autowired
	private JdbcTemplate jdbc;
	
	// テストで繰り返し使う日時を固定値として定義
	// 更新前の作成日時など
	private static final LocalDateTime OLD_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
	// 予約の開始日時
	private static final LocalDateTime START = LocalDateTime.of(2030, 4, 1, 10, 0);
	
	// テストごとにデータベースを初期化する
	@BeforeEach
	void setUp() {
		// 外部キーの参照"元"から削除。
		jdbc.update("DELETE FROM reservations");
		jdbc.update("DELETE FROM users");
		jdbc.update("DELETE FROM goal_levels");
		// 目標レベルを登録する
		jdbc.update("""
				INSERT INTO goal_levels (id, level_code, display_order, enabled)
				VALUES (1, 'N1', 1, TRUE), (2, 'N2', 2, TRUE)
				""");
	}
	
	@Test
	void findByIdで予約2件と目標レベルを取得できる() {
		
		insertUser(101L, "山田太郎", 1);
		insertUser(102L, "別ユーザー", 2);
		insertReservation(201L, 101L, START);
		insertReservation(202L, 101L, START.plusDays(1));
		insertReservation(203L, 102L, START.plusDays(2));
		
		User actual = mapper.findById(101L);
		
		assertNotNull(actual);
		assertEquals("山田太郎", actual.getUserName());
		assertEquals(1, actual.getGoalLevelId());
		assertNotNull(actual.getGoalLevel());
		assertEquals("N1", actual.getGoalLevel().getLevelCode());
		assertNotNull(actual.getReservationList());
		assertEquals(2, actual.getReservationList().size());
		
		List<Reservation> reservations = actual.getReservationList();
		assertReservation(reservations.get(0), 201L, 101L, START);
		assertReservation(reservations.get(1), 202L, 101L, START.plusDays(1));
	}
	
	@Test
	void findByIdで予約がない場合は空の予約一覧になる() {
		
		insertUser(101L, "山田太郎", 1);
		
		User actual = mapper.findById(101L);
		
		assertNotNull(actual);
		assertNotNull(actual.getReservationList());
		assertTrue(actual.getReservationList().isEmpty());
	}
	
	@Test
	void findByIdで存在しないIDはNullになる() {
		assertNull(mapper.findById(-1L));
	}
	
	@Test
	void insertOneで登録内容と自動採番IDと日時が保存される() {
		
		User user = new User();
		user.setE164PhoneNumber("+819012345678");
		user.setUserName("登録テスト");
		user.setPassword("test1234");
		user.setGoalLevelId(1);
		user.setRole("ROLE_GENERAL");
		
		assertEquals(1, mapper.insertOne(user));
		assertNotNull(user.getUserId());
		
		User actual = mapper.findById(user.getUserId());
		assertNotNull(actual);
		assertAll(
				() -> assertEquals(user.getUserId(), actual.getUserId()),
				() -> assertEquals(user.getE164PhoneNumber(), actual.getE164PhoneNumber()),
				() -> assertEquals(user.getUserName(), actual.getUserName()),
				() -> assertEquals(1, actual.getGoalLevelId()),
				() -> assertEquals("ROLE_GENERAL", actual.getRole()),
				() -> assertNotNull(actual.getCreatedAt()),
				() -> assertNotNull(actual.getUpdatedAt())
		);
		// findByIdはpasswordを取得しないため、DBから直接確認する
		assertEquals("test1234", passwordOf(user.getUserId()));
		
	}
	
	@Test
	void insertOneで電話番号の重複は拒否される() {
		
		insertUser(101L, "登録済み", 1);
		
		User user = new User();
		user.setE164PhoneNumber(phoneOf(101L));
		user.setUserName("重複ユーザー");
		user.setPassword("test1234");
		user.setGoalLevelId(1);
		user.setRole("ROLE_GENERAL");
		
		assertThrows(DataIntegrityViolationException.class, () -> mapper.insertOne(user));
		assertEquals(1, mapper.count(new User()));
	}
	
	@Test
	void findManyとcountで検索条件が一致する() {
		
		insertUser(101L, "山田太郎", 1);
		insertUser(102L, "山田花子", 2);
		insertUser(103L, "佐藤太郎", 1);
		
		assertSearch(null, null, List.of(101L, 102L, 103L));
		assertSearch("", null, List.of(101L, 102L, 103L));
		assertSearch("山田", null, List.of(101L, 102L));
		assertSearch(null, 1, List.of(101L, 103L));
		assertSearch("山田", 1, List.of(101L));
		assertSearch("該当なし", null, List.of());
	}
	
	@Test
	void findManyでページを切り替えても重複せず総数は変わらない() {
		
		for (long id = 101; id <= 105; id++) {
			insertUser(id, "ユーザー" + id, 1);
		}
		User condition = new User();
		
		assertEquals(
				List.of(101L, 102L, 103L),
				ids(mapper.findMany(condition, PageRequest.of(0, 3)))
		);
		assertEquals(
				List.of(104L, 105L),
				ids(mapper.findMany(condition, PageRequest.of(1, 3)))
		);
		assertTrue(mapper.findMany(condition, PageRequest.of(2, 3)).isEmpty());
		assertEquals(5, mapper.count(condition));
	}
	
	@Test
	void updateByIdで指定ユーザーだけ更新し作成日時は維持する() {
		
		insertUser(101L, "更新前", 1);
		insertUser(102L, "変更しない", 2);
		
		assertEquals(1, mapper.updateById(101L, "newPassword", "更新後"));
		
		User actual = mapper.findById(101L);
		assertAll(
				() -> assertEquals("更新後", actual.getUserName()),
				() -> assertEquals("newPassword", passwordOf(101L)),
				() -> assertEquals(OLD_TIME, actual.getCreatedAt()),
				() -> assertTrue(actual.getUpdatedAt().isAfter(OLD_TIME)),
				() -> assertEquals(phoneOf(101L), actual.getE164PhoneNumber()),
				() -> assertEquals(1, actual.getGoalLevelId())
		);
		User other = mapper.findById(102L);
		assertEquals("変更しない", other.getUserName());
		assertEquals("beforePassword", passwordOf(102L));
		assertEquals(OLD_TIME, other.getUpdatedAt());
	}
	
	@Test
	void updateByIdで存在しないIDは更新件数0になる() {
		assertEquals(0, mapper.updateById(-1L, "pssword", "名前"));
	}
	
	@Test
	void deleteByIdで予約がないユーザーは削除できる() {
		
		insertUser(101L, "削除対象", 1);
		insertUser(102L, "残すユーザー", 2);
		
		assertEquals(1, mapper.deleteById(101L));
		assertNull(mapper.findById(101L));
		assertNotNull(mapper.findById(102L));
		assertEquals(1, mapper.count(new User()));
	}
	
	
	/* 共通で処理するメソッド */
	// 取得・更新テストの準備には自作したinsertOneを使わず登録処理の不具合と切り離す。
	private void insertUser(long id, String name, int goalId ) {
		jdbc.update("""
				INSERT INTO users
					(user_id, e164_phone_number, user_name, password, goal_level_id, role, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""", id, phoneOf(id), name, "beforePassword", goalId, "ROLE_GENERAL", OLD_TIME, OLD_TIME);

	}
	
	private String phoneOf(long id) {
		return "+819000000" + id;
	}
	
	private String passwordOf(long userId) {
		return jdbc.queryForObject(
				"SELECT password FROM users WHERE user_id = ?", String.class, userId);
	}
	
	private void insertReservation(long id, long userId, LocalDateTime start) {
		jdbc.update("""
				INSERT INTO reservations
					(reservation_id, user_id, starts_at, ends_at, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?)
				""", id, userId, start, start.plusHours(1), OLD_TIME, OLD_TIME);
	}
	
	private List<Long> ids(List<User> users) {
		
		List<Long> userIds = new ArrayList<>();
		
		for (User user : users) {
			userIds.add(user.getUserId());
		}
		
		return userIds;
	}
	
	private void assertSearch(String name, Integer goalId, List<Long> expectedIds) {
		
		User condition = new User();
		condition.setUserName(name);
		condition.setGoalLevelId(goalId);
		
		assertEquals(expectedIds, ids(mapper.findMany(condition, PageRequest.of(0, 100))));
	}
	
	private void assertReservation(Reservation actual, long id, long userId, LocalDateTime start) {
		
		assertAll(
				() -> assertEquals(Long.valueOf(id), actual.getReservationId()),
				() -> assertEquals(Long.valueOf(userId), actual.getUserId()),
				() -> assertEquals(start, actual.getStartsAt()),
				() -> assertEquals(start.plusHours(1), actual.getEndsAt()),
				() -> assertEquals(OLD_TIME, actual.getCreatedAt()),
				() -> assertEquals(OLD_TIME, actual.getUpdatedAt())
		);
	}
}
