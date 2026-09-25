package com.ShineYamamoto.LessonManagerApp.user.form;

import java.time.LocalDateTime;
import java.util.List;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;

import lombok.Data;

@Data
public class UserDetailForm {
	
	private Long userId;
	private String e164PhoneNumber;
	private String password;
	private String userName;
	private Integer goalLevelId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<Reservation> reservationList;
}
