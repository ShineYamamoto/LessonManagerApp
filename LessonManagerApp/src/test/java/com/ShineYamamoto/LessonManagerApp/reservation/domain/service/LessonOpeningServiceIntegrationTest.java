package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningNotFoundException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.user.repository.UserMapper;

@SpringBootTest
@Transactional(isolation = Isolation.READ_COMMITTED)
public class LessonOpeningServiceIntegrationTest {

	@Autowired
	private LessonOpeningService lessonOpeningService;
	
	@Autowired ReservationService reservationService;
	
	@Autowired
	private UserMapper userMapper;
	
	@Test
	void 受付可能時間を登録更新削除ができる() {
		
		LocalDate date = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(90);
		
		LocalDateTime startsAt = date.atTime(10, 0);
		LocalDateTime endsAt = date.atTime(12, 0);
		
		// 登録
		Long openingId = lessonOpeningService.create(startsAt, endsAt);
		
		assertNotNull(openingId);
		
		LessonOpening created = lessonOpeningService.getById(openingId);
		
		assertEquals(openingId, created.getOpeningId());
		assertEquals(startsAt, created.getStartsAt());
		assertEquals(endsAt, created.getEndsAt());
		
		// 更新
		LocalDateTime updatedStart = date.atTime(14, 0);
		LocalDateTime updatedEnd = date.atTime(18, 0);
		
		lessonOpeningService.update(openingId, updatedStart, updatedEnd, 0L);
		
		LessonOpening updated = lessonOpeningService.getById(openingId);
		
		// 更新してもIDは変わらない
		assertEquals(openingId, updated.getOpeningId());
		assertEquals(updatedStart, updated.getStartsAt());
		assertEquals(updatedEnd, updated.getEndsAt());
		
		// 同じ内容で更新しても正常終了する
		assertDoesNotThrow(() -> lessonOpeningService.update(openingId, updatedStart, updatedEnd, 1L));
		
		// 削除
		lessonOpeningService.delete(openingId, 1L);
		
		assertThrows(
				LessonOpeningNotFoundException.class,
				() -> lessonOpeningService.getById(openingId));
		
		// 削除済みのデータを削除すると例外になる
		assertThrows(
				LessonOpeningNotFoundException.class,
				() -> lessonOpeningService.delete(openingId, 1L));
	}
	
	@Test
	void 隣接する受付時間は登録できるが重複は拒否される() {
		
		LocalDate date = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(90);
		
		lessonOpeningService.create(
				date.atTime(10, 0),
				date.atTime(12, 0));
		
		// 12:00で接するだけなので登録できる
		assertDoesNotThrow(() ->
				lessonOpeningService.create(
						date.atTime(12, 0),
						date.atTime(14, 0)));
		
		// 11:30～12:00が既存の受付時間と重複する
		LessonOpeningCheckException exception = assertThrows(
				LessonOpeningCheckException.class,
				() -> lessonOpeningService.create(
						date.atTime(11, 30),
						date.atTime(13, 0)));
		
		assertEquals(
				"lessonOpening.error.overlap",
				exception.getMessageKey());
	}
	
	@Test
	void 受付時間をまたぐ確定予約を受付可能時間の更新と削除から保護できる() {
		
		LocalDate date = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(90);
		
		Long firstId = lessonOpeningService.create(
				date.atTime(10, 0),
				date.atTime(11, 0));
		
		Long secondId = lessonOpeningService.create(
				date.atTime(11, 0),
				date.atTime(12, 0));
		
		// data.sqlにある60分授業のテスト用ユーザー
		Long userId = userMapper
				.findByE164PhoneNumber("+817000000001")
				.getUserId();
		
		// 10:30-11:30の予約。2つの受付枠をまたぐ
		reservationService.reserve(
				userId,
				List.of(date.atTime(10, 30)));
		
		// 受付終了を伸ばすだけなら、予約を維持する
		assertDoesNotThrow(() ->
				lessonOpeningService.update(
						secondId,
						date.atTime(11, 0),
						date.atTime(13, 0),
						0L));
		
		// 11:00～11:30に隙間ができる変更は拒否される
		LessonOpeningCheckException updateError = assertThrows(
				LessonOpeningCheckException.class,
				() -> lessonOpeningService.update(
						secondId,
						date.atTime(11, 30),
						date.atTime(13, 0),
						1L));
		
		assertEquals(
				"lessonOpening.error.reservationExists",
				updateError.getMessageKey());
		
		// 拒否された変更がDBに反映されていない
		assertEquals(
				date.atTime(11, 0),
				lessonOpeningService.getById(secondId).getStartsAt());
		
		// 最初の受付枠を削除すると10:30～11:00が欠ける
		LessonOpeningCheckException deleteError = assertThrows(
				LessonOpeningCheckException.class,
				() -> lessonOpeningService.delete(firstId, 0L));
		
		assertEquals(
				"lessonOpening.error.reservationExists",
				deleteError.getMessageKey());
		
		// 削除されずに残っている
		assertNotNull(lessonOpeningService.getById(firstId));
	}
}
