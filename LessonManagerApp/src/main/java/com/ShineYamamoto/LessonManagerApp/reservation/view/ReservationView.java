package com.ShineYamamoto.LessonManagerApp.reservation.view;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReservationView {

	private Long id;
	
	private LocalDateTime startsAt;
	
	private LocalDateTime endsAt;
	
	// 日時変更が可能か
	private boolean editable;
	
	// キャンセル可能か
	private boolean cancellable;
}
