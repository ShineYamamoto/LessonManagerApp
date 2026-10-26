package com.ShineYamamoto.LessonManagerApp.reservation;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationChangeResult;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationService;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationMapper;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reservation_change_test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
public class ReservationChangeIntegrationTest {

	private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
	
	private static final String RESULT_KEY = "reservation.change.result";
	
	private static final LocalDateTime OLD_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ReservationService reservationService;
	
	@Autowired
	private ReservationMapper reservationMapper;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	private Long userA;
	private Long userB;
	private Long reservationId;
	private LocalDate date;
	
	@BeforeEach
	void setUp() {
		
		// このテスト専用H2のデータだけを初期化する
		jdbcTemplate.update("DELETE FROM reservations");
		jdbcTemplate.update("DELETE FROM lesson_openings");
		
		userA = findUserId("+817000000001");
		userB = findUserId("+817000000002");
		
		jdbcTemplate.update(
				"UPDATE users SET lesson_minutes = 60"
					+ "WHERE user_id IN (?, ?)",
				userA,
				userB);
		
		date = LocalDate.now(JAPAN_ZONE).plusDays(60);
		
		jdbcTemplate.update(
				"""
				INSERT INTO lesson_openings (starts_at, ends_at)
				VALUES (?, ?)
				""",
				timestamp(at(9)),
				timestamp(at(22)));
		
		// Aの元の予約: 10:00～11:00
		reservationId = insertReservation(userA, at(10));
	}
	
	@Test
	void 正常変更で元の授業時間を維持し完了画面を表示する() throws Exception {
		
		// 現在のユーザーのlessonMinutesを30分に変更しておく
		// それでも元の予約の60分が維持されることを確認する
		jdbcTemplate.update(
				"UPDATE users SET lesson_minutes = 30 WHERE user_id = ?",
				userA);
		
		MockHttpSession session = new MockHttpSession();
		
		LocalDateTime requestedStart = at(10).plusMinutes(30);
		LocalDateTime requestedEnd =at(11).plusMinutes(30);
		
		mockMvc.perform(post(editUrl())
						.with(user(loginUser(userA)))
						.with(csrf())
						.session(session)
						.param("startsAt", requestedStart.toString())
						.param("expectedVersion", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(completeUrl(reservationId, 1L)));
		
		Reservation after = read(reservationId, userA);
		
		assertEquals(requestedStart, after.getStartsAt());
		assertEquals(requestedEnd, after.getEndsAt());
		assertEquals(Long.valueOf(1), after.getVersion());
		
		// 更新者を記録し、作成時の情報は維持する
		assertEquals(userA, after.getUpdatedBy());
		assertEquals(userB, after.getCreatedBy());
		assertEquals(OLD_TIME, after.getCreatedAt());
		assertNotNull(after.getUpdatedAt());
		assertNotEquals(OLD_TIME, after.getUpdatedAt());
		
		assertEquals(1, countAll());
		
		Object stored = session.getAttribute(RESULT_KEY);
		assertInstanceOf(ReservationChangeResult.class, stored);
		
		ReservationChangeResult result = (ReservationChangeResult) stored;
		
		assertEquals(reservationId, result.getReservationId());
		assertEquals(userA, result.getUserId());
		assertEquals(Long.valueOf(1), result.getVersion());
		assertEquals(at(10), result.getBeforeStartsAt());
		assertEquals(at(11), result.getBeforeEndsAt());
		assertEquals(requestedStart, result.getAfterStartsAt());
		assertEquals(requestedEnd, result.getAfterEndsAt());
		
		// 再読み込みしても表示だけを行い、更新しない
		for (int i = 0; i < 2; i++) {
			
			mockMvc.perform(get(completeUrl(reservationId, 1L))
							.with(user(loginUser(userA)))
							.session(session))
					.andExpect(view().name("reservation/reservation-change-complete"))
					.andExpect(model().attribute("changeResult", result))
					.andExpect(content().string(containsString("10:00")))
					.andExpect(content().string(containsString("11:00")))
					.andExpect(content().string(containsString("10:30")))
					.andExpect(content().string(containsString("11:30")));	
		}
		
		assertEquals(after, read(reservationId, userA));
	}
	
	@Test
	void 他人の予約は編集画面を開けず変更もできない() throws Exception {
		
		Reservation before = read(reservationId, userA);
		
		mockMvc.perform(get(editUrl())
						.with(user(loginUser(userB))))
				.andExpect(status().isNotFound());
		
		mockMvc.perform(post(editUrl())
						.with(user(loginUser(userB)))
						.with(csrf())
						.param("startsAt", at(12).toString())
						.param("expectedVersion", "0"))
				.andExpect(status().isNotFound());
	
		assertEquals(before, read(reservationId, userA));
		assertEquals(1, countAll());
	}
	
	@Test
	void 重複する変更は保存せず候補を編集画面へ返す() throws Exception {
		
		Long otherId = insertReservation(userB, at(13));
		
		Reservation before= read(reservationId, userA);
		Reservation otherBefore = read(otherId, userB);
		
		MockHttpSession session = new MockHttpSession();
		
		mockMvc.perform(post(editUrl())
						.with(user(loginUser(userA)))
						.with(csrf())
						.session(session)
						.param("startsAt", at(13).toString())
						.param("expectedVersion", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(editUrl()))
				.andExpect(flash().attributeExists("reservationError"))
				.andExpect(flash().attribute("restoredStarts", at(13).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
		
		assertEquals(before, read(reservationId, userA));
		assertEquals(otherBefore, read(otherId, userB));
		assertEquals(2, countAll());
		
		// 失敗した操作の完了情報を保存していない
		assertNull(session.getAttribute(RESULT_KEY));
	}
	
	@Test
	void 古い更新番号では先に成功した変更を上書きできない() {
		
		reservationService.change(userA, reservationId, at(12), 0L);
		
		Reservation afterFirstChange = read(reservationId, userA);
		
		ReservationCheckException exception = assertThrows(
				ReservationCheckException.class,
				() -> reservationService.change(userA, reservationId, at(14), 0L));
		
		assertEquals("reservation.change.error.conflict", exception.getMessageKey());
		assertEquals(afterFirstChange, read(reservationId, userA));
		assertEquals(at(12), afterFirstChange.getStartsAt());
		assertEquals(Long.valueOf(1), afterFirstChange.getVersion());
		assertEquals(1, countAll());
	}
	
	// ---------- 共通の準備・確認処理 ----------
	
	private Long findUserId(String phone) {
		return jdbcTemplate.queryForObject(
				"SELECT user_id FROM users WHERE e164_phone_number = ?",
				Long.class,
				phone);
	}
	
	private Long insertReservation(
			Long ownerId,
			LocalDateTime startsAt) {
		
		jdbcTemplate.update(
				"""
				INSERT INTO reservations (
					user_id, starts_at, ends_at, status, created_by, updated_by, created_at, updated_at
				)
				VALUES (?, ?, ?, 'CONFIRMED', ?, ?, ?, ?)
				""",
				ownerId,
				timestamp(startsAt),
				timestamp(startsAt.plusHours(1)),
				// 更新者の変更を検証するため、作成者・更新者は意図的にBにする
				userB,
				userB,
				timestamp(OLD_TIME),
				timestamp(OLD_TIME));
		
		return jdbcTemplate.queryForObject(
				"""
				SELECT reservation_id
				FROM reservations
				WHERE user_id = ? AND starts_at = ?
				""",
				Long.class,
				ownerId,
				timestamp(startsAt));
	}
	
	private Reservation read(Long id, Long ownerId) {
		
		Reservation reservation = reservationMapper.findUpcomingByIdAndUserId(
				id,
				ownerId,
				LocalDateTime.now(JAPAN_ZONE));
		
		assertNotNull(reservation);
		return reservation;
	}
	
	private LoginUser loginUser(Long id) {
		
		String phone = id.equals(userA)
				? "+817000000001"
				: "+817000000002";
		
		return new LoginUser(
				id,
				phone,
				"unused",
				List.of(new SimpleGrantedAuthority("ROLE_GENERAL")),
				"テストユーザー");
	}
	
	private LocalDateTime at(int hour) {
		return date.atTime(hour, 0);
	}
	
	private Timestamp timestamp(LocalDateTime value) {
		return Timestamp.valueOf(value);
	}
	
	private String editUrl() {
		return "/user/reservation/" + reservationId + "/edit";
	}
	
	private String completeUrl(Long id, Long version) {
		return "/user/reservation/" + id + "/change-complete/" + version;
	}
	
	private int countAll() {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM reservations",
				Integer.class);
	}
}
