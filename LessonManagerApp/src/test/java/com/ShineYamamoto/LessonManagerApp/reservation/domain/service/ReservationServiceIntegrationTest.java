package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:reservation_service_test;DB_CLOSE_DELAY=-1",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.sql.init.mode=always"
})
public class ReservationServiceIntegrationTest {

	@Autowired
	private ReservationService reservationService;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private Long userId;
	private LocalDate date;
	
	@BeforeEach
	void setUp() {
		
		// data.sqlに存在するテスト用ユーザー
		userId = jdbcTemplate.queryForObject(
				"""
				SELECT user_id
				FROM users
				WHERE e164_phone_number = ?
				""",
				Long.class,
				"+817000000001");
		
		date= LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(120);
		
		// 9時から13時までを予約可能にする
		jdbcTemplate.update(
				"""
				INSERT INTO lesson_openings
					(starts_at, ends_at)
				VALUES (?, ?)
				""",
				Timestamp.valueOf(at(9)),
				Timestamp.valueOf(at(13)));
	}
	
	@AfterEach
	void tearDown() {
		
		// 外部キー参照される予約から削除する
		jdbcTemplate.update(
				"""
				DELETE FROM reservations
				WHERE user_id = ?
					AND starts_at >= ?
					AND ends_at < ?
				""",
				userId,
				Timestamp.valueOf(date.atStartOfDay()),
				Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
		
		jdbcTemplate.update(
				"""
				DELETE FROM lesson_openings
				WHERE starts_at = ?
					AND ends_at = ?
				""",
				Timestamp.valueOf(at(9)),
				Timestamp.valueOf(at(13)));
	}
	
	@Test
	void 正常な2件の予約がDBに登録されている() {
		
		List<ReservationSlot> result = reservationService.reserve(userId, List.of(at(10), at(11)));
		
		assertEquals(2, result.size());
		
		// DB上でも2件登録されているか確認
		assertEquals(2, countReservations());
		
		// ステータスと編集者情報も正しいか確認
		Integer correctRows = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM reservations
				WHERE user_id = ?
					AND starts_at >= ?
					AND starts_at < ?
					AND status = 'CONFIRMED'
					AND created_by = ?
					AND updated_by = ?
				""",
				Integer.class,
				userId,
				Timestamp.valueOf(date.atStartOfDay()),
				Timestamp.valueOf(date.plusDays(1).atStartOfDay()),
				userId,
				userId);
		
		assertNotNull(correctRows);
		assertEquals(2, correctRows.intValue());
	}
	
	@Test
	void ひとつでも予約不可ならDBになにも登録されない() {
		
		// 10時は予約可能
		// 14時は受付時間外
		assertThrows(
				ReservationCheckException.class,
				() -> reservationService.reserve(userId, List.of(at(10), at(14))));
		
		// 正常だった10時の予約も登録されていない
		assertEquals(0, countReservations());
		
	}
	
	// ---------- テスト用の共通処理 ----------
	
	private LocalDateTime at(int hour) {
		return date.atTime(hour, 0);
	}
	
	private int countReservations() {
		
		Integer count = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM reservations
				WHERE user_id = ?
					AND starts_at >= ?
					AND ends_at < ?
				""",
				Integer.class,
				userId,
				Timestamp.valueOf(date.atStartOfDay()),
				Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
		
		assertNotNull(count);
		return count.intValue();
	}
}
