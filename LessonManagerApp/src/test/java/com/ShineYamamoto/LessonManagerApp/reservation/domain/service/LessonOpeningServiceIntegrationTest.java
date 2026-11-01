package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
		
		lessonOpeningService.update(openingId, updatedStart, updatedEnd);
		
		LessonOpening updated = lessonOpeningService.getById(openingId);
		
		// 更新してもIDは変わらない
		assertEquals(openingId, updated.getOpeningId());
		assertEquals(updatedStart, updated.getStartsAt());
		assertEquals(updatedEnd, updated.getEndsAt());
		
		// 同じ内容で更新しても正常終了する
		assertDoesNotThrow(() -> lessonOpeningService.update(openingId, updatedStart, updatedEnd));
		
		// 削除
		lessonOpeningService.delete(openingId);
		
		assertThrows(
				LessonOpeningNotFoundException.class,
				() -> lessonOpeningService.getById(openingId));
		
		// 削除済みのデータを削除すると例外になる
		assertThrows(
				LessonOpeningNotFoundException.class,
				() -> lessonOpeningService.delete(openingId));
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
}
