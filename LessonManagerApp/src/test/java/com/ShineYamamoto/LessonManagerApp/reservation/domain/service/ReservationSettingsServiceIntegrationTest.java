package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSettings;

@SpringBootTest
@Transactional
public class ReservationSettingsServiceIntegrationTest {

	@Autowired
	private ReservationSettingsService reservationSettingsService;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;
	
	@Test
	void DBの時間帯設定を取得し変更後の値も取得できる() {
		
		// テスト開始時の設定を明示する
		updateSettings("09:00:00", "22:00:00");
		
		ReservationSettings before = reservationSettingsService.getSettings();
		
		assertEquals(LocalTime.of(9, 0), before.getSelectableStartTime());
		
		assertEquals(LocalTime.of(22, 0), before.getSelectableEndTime());
		
		// DBの設定を変更する
		updateSettings("08:30:00", "23:00:00");
		
		ReservationSettings after = reservationSettingsService.getSettings();
		
		assertEquals(LocalTime.of(8, 30), after.getSelectableStartTime());
		
		assertEquals(LocalTime.of(23, 0), after.getSelectableEndTime());
	}
	
	private void updateSettings(String start, String end) {
		
		int updated = jdbcTemplate.update(
				"""
				UPDATE reservation_settings
				SET selectable_start_time = ?
				  , selectable_end_time = ?
				 WHERE settings_id = 1
				""",
				java.sql.Time.valueOf(start),
				java.sql.Time.valueOf(end));
		
		assertEquals(1, updated);
		
		// JdbcTemplateで更新したため、MyBatisの取得済みデータを消去する
		sqlSessionTemplate.clearCache();
	}
}
