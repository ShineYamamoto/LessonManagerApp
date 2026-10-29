package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSettings;
import com.ShineYamamoto.LessonManagerApp.reservation.repository.ReservationSettingsMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationSettingsService {

	// 時間の刻み幅は30分単位
	private static final int STEP_MINUTES = 30;
	
	private final ReservationSettingsMapper reservationSettingsMapper;
	
	/** 検証済みの予約設定を取得する */
	public ReservationSettings getSettings() {
		
		ReservationSettings settings = reservationSettingsMapper.findSettings();
		
		if (settings == null) {
			throw new IllegalStateException("予約の時間帯設定が登録されていません");
		}
		
		LocalTime start = settings.getSelectableStartTime();
		LocalTime end = settings.getSelectableEndTime();
		
		if (start == null || end == null) {
			throw new IllegalStateException("予約の時間帯設定に開始・終了時間がありません");
		}
		
		if (!start.isBefore(end)) {
			throw new IllegalStateException("予約の時間帯設定は、開始時刻を終了時刻より前にしてください。");
		}
		
		if (!isStepAligned(start) || !isStepAligned(end)) {
			throw new IllegalStateException("予約の時間帯設定は30分単位にしてください");
		}
		
		return settings;
	}
	
	/** 秒・小数秒を含まない、30分単位の時刻か */
	private boolean isStepAligned(LocalTime time) {
		
		return time.getMinute() % STEP_MINUTES == 0
				&& time.getSecond() == 0
				&& time.getNano() == 0;
	}
}
