package com.ShineYamamoto.LessonManagerApp.reservation.form;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ReservationForm {

	/** まとめてクライアントから送信された予約希望 */
	@Valid
	@NotEmpty
	private List<@NotNull ReservationItemForm> reservationList = new ArrayList<>();
}
