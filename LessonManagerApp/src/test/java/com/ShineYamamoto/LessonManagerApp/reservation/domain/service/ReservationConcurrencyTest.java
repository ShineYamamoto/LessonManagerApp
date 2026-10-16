package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

@SpringBootTest
public class ReservationConcurrencyTest {

	@Autowired
	private ReservationService reservationService;
	
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Test
	void 同じ時間に2人が予約しても1件しか登録されない() throws Exception {
		
		Long userA = userMapper
				.findByE164PhoneNumber("+817000000001")
				.getUserId();
		
		Long userB = userMapper
				.findByE164PhoneNumber("+817000000002")
				.getUserId();
		
		LocalDate date = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(30);
		
		LocalDateTime startsAt = date.atTime(10, 0);
		LocalDateTime endsAt = date.atTime(11, 0);
		
		LocalDateTime openingStart = date.atTime(9, 0);
		LocalDateTime openingEnd = date.atTime(12, 0);
		
		Timestamp start = Timestamp.valueOf(startsAt);
		Timestamp end = Timestamp.valueOf(endsAt);
		
		// テスト用の受付可能時間を作成する
		jdbcTemplate.update(
				"INSERT INTO lesson_openings "
				+ "(starts_at, ends_at) VALUES (?, ?)",
				Timestamp.valueOf(openingStart),
				Timestamp.valueOf(openingEnd));
		
		ExecutorService executor = Executors.newFixedThreadPool(2);
		
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch startSignal = new CountDownLatch(1);
		
		try {
			Callable<Boolean> taskA = createTask(userA, startsAt, ready, startSignal);
			Callable<Boolean> taskB = createTask(userB, startsAt, ready, startSignal);
			
			Future<Boolean> resultA = executor.submit(taskA);
			Future<Boolean> resultB = executor.submit(taskB);
			
			// 2スレッドの準備が完了するまで待つ
			assertTrue(ready.await(10, TimeUnit.SECONDS));
			
			// 同時に予約処理を開始する
			startSignal.countDown();
			
			boolean successA = resultA.get(20, TimeUnit.SECONDS);
			boolean successB = resultB.get(20, TimeUnit.SECONDS);
			
			// 成功した予約は1件だけ
			int successCount = (successA ? 1 : 0) + (successB ? 1 : 0);
			
			assertEquals(1, successCount);
			
			// DBにも1件だけ登録されている
			Integer dbCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM reservations "
					+ "WHERE starts_at = ? "
					+ "AND ends_at = ? "
					+ "AND status = 'CONFIRMED'",
					Integer.class,
					start,
					end);
			
			assertEquals(1, dbCount);
			
		} finally {
			
			startSignal.countDown();
			executor.shutdownNow();
			
			// テストデータの削除
			jdbcTemplate.update(
					"DELETE FROM reservations "
					+ "WHERE starts_at = ? AND ends_at = ?",
					start,
					end);
			
			jdbcTemplate.update(
					"DELETE FROM lesson_openings "
					+ "WHERE starts_at = ? AND ends_at = ?",
					Timestamp.valueOf(openingStart),
					Timestamp.valueOf(openingEnd));
		}
	}
	
	private Callable<Boolean> createTask(
			Long userId,
			LocalDateTime startsAt,
			CountDownLatch ready,
			CountDownLatch startSignal) {
		
		return () -> {
			ready.countDown();
			
			if (!startSignal.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException(
						"予約開始待機がタイムアウトしました。");
			}
			
			try {
				reservationService.reserve(
						userId,
						List.of(startsAt));
				
				return true;
				
			} catch (ReservationCheckException e) {
				return false;
			}
		};
	}
}
