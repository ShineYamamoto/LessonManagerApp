package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.LessonOpeningNotFoundException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;

@SpringBootTest
@Transactional
public class LessonOpeningServiceIntegrationTest {

	@Autowired
	private LessonOpeningService lessonOpeningService;
	
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
}
