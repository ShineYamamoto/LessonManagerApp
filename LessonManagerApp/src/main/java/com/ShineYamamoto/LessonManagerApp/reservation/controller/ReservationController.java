package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class ReservationController {
	/** ホーム画面を表示 */
	@GetMapping("/reservation/single")
	public String getReservation() {

		return "reservation/reservation-single";
	}
}
