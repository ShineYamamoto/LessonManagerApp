package com.ShineYamamoto.LessonManagerApp.reservation.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationProblem;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationDataService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationService;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.service.ReservationSettingsService;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationForm;
import com.ShineYamamoto.LessonManagerApp.reservation.form.ReservationItemForm;
import com.ShineYamamoto.LessonManagerApp.security.LoginUser;

@ExtendWith(MockitoExtension.class)
public class reservationControllerTest {

	private static final Long USER_ID = 2L;
	
	@Mock
	private ReservationDataService reservationDataService;
	
	@Mock
	private ReservationService reservationService;
	
	@Mock
	private ReservationSettingsService reservationSettingsService;
	
	private ReservationController reservationController;
	
	private LoginUser loginUser;
	private LocalDate date;
	
	@BeforeEach
	void setUp() {
		
		// 実際のpropertiesファイルを読み込む
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		
		messageSource.setBasenames("messages", "ValidationMessages");
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setFallbackToSystemLocale(false);
		
		reservationController = new ReservationController(
				reservationDataService,
				reservationService,
				messageSource,
				reservationSettingsService);
		
		loginUser = mock(LoginUser.class);
		
		when(loginUser.getUserId())
			.thenReturn(USER_ID);
		
		date = LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(90);
	}
	
	@Test
	void 予約成功時はセッションに結果を保存し完了画面へ進む() {
		
		LocalDateTime start = date.atTime(13, 0);
		
		ReservationSlot slot = new ReservationSlot(start, start.plusHours(1));
		
		ReservationForm form = form(start);
		
		when(reservationService.reserve(USER_ID, List.of(start)))
			.thenReturn(List.of(slot));
		
		MockHttpSession session = new MockHttpSession();
		
		String result = reservationController.postReservation(
				form,
				new BeanPropertyBindingResult(form, "reservationForm"),
				loginUser, 
				session,
				new RedirectAttributesModelMap(),
				Locale.JAPANESE);
		
		// リダイレクト先が正しい
		assertEquals("redirect:/user/reservation/complete", result);
		
		// セッションへ正しい予約結果が保存された
		assertEquals(USER_ID, session.getAttribute("reservation.completedUserId"));
		assertEquals(List.of(slot), session.getAttribute("reservation.completedSlots"));
		
		// 完了画面でも予約結果を取得できる
		ExtendedModelMap model = new ExtendedModelMap();
		
		assertEquals(
				"reservation/reservation-complete",
				reservationController.getReservationComplete(loginUser, session, model));
		assertEquals(List.of(slot), model.get("reservedSlots"));
		
		// 別のログインユーザー二は表示されない
		LoginUser otherUser = mock(LoginUser.class);
		
		when(otherUser.getUserId()).thenReturn(999L);
		
		assertEquals(
				"redirect:/user/reservation/single",
				reservationController.getReservationComplete(otherUser, session, new ExtendedModelMap()));
	}
	
	@Test
	void 予約失敗時は正常な候補だけ保持し中国語エラーを返す() {
		
		LocalDateTime failed = date.atTime(10, 0);
		LocalDateTime valid = date.atTime(11, 0);
		
		ReservationForm form = form(failed, valid);
		
		ReservationCheckException exception = new ReservationCheckException(
				List.of(new ReservationProblem(failed, "reservation.error.booked")));
		
		when(reservationService.reserve(USER_ID, List.of(failed, valid)))
			.thenThrow(exception);
		
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
		
		String result = reservationController.postReservation(
				form,
				new BeanPropertyBindingResult(form, "reservationForm"),
				loginUser,
				new MockHttpSession(),
				redirectAttributes,
				Locale.SIMPLIFIED_CHINESE);
		
		assertEquals("redirect:/user/reservation/single", result);
		
		// 正常な11時だけ復元対象になった
		String expectedStarts = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(valid);
		
		assertEquals(expectedStarts, redirectAttributes.getFlashAttributes().get("restoredStarts"));
		
		// エラーメッセージが中国語になっている
		String message = (String) redirectAttributes.getFlashAttributes().get("reservationError");
		
		assertNotNull(message);
		assertTrue(message.contains("无法预约"));
	}
	
	// ---------- テスト用Form生成 ----------
	
	private ReservationForm form(LocalDateTime... starts) {
		
		List<ReservationItemForm> items = java.util.Arrays.stream(starts)
				.map(start -> {
					
					ReservationItemForm item = new ReservationItemForm();
					
					item.setStartsAt(start);
					
					return item;
				})
				.toList();
		ReservationForm form = new ReservationForm();
		form.setReservationList(items);
		
		return form;
	}
	
}
