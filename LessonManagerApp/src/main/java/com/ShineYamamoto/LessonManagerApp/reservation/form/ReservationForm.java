package com.ShineYamamoto.LessonManagerApp.reservation.form;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import lombok.Data;

@Data
public class ReservationForm {

	/** まとめてクライアントから送信された予約希望 */
	@Valid
	@NotEmpty
	private List<ReservationItemForm> reservationList = new ArrayList<>();
}
