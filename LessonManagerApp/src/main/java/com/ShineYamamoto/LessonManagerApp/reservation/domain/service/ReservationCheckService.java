package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationCheckException;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.LessonOpening;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.Reservation;
import com.ShineYamamoto.LessonManagerApp.reservation.domain.model.ReservationSlot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationCheckService {
	
	private static final ZoneId BOOKING_ZONE = ZoneId.of("Asia/Tokyo");
	
	private static final int STEP_MINUTES = 30;
	private static final int OPEN_MINUTES = 9 * 60;
	private static final int CLOSE_MINUTES = 22 * 60;
	
	private final ReservationDataService reservationDataService;
	
	/**
	 * 予約希望を検証する。
	 * 全件問題なければ、サーバーで終了日時を計算した時間帯を返す。
	 */
	public List<ReservationSlot> check(
			Long userId,
			List<LocalDateTime> requestedStarts) {
		
		if (requestedStarts == null || requestedStarts.isEmpty()) {
			throw new ReservationCheckException(
					"予約希望日時を1件以上選択してください。");
		}
		
		// 授業時間はブラウザの値ではなくDBから取得する
		int lessonMinutes = reservationDataService.getLessonMinutes(userId);
		
		// 正の30分単位であることは既存DataServiceで検証済み
		if (lessonMinutes > CLOSE_MINUTES - OPEN_MINUTES) {
			throw new IllegalStateException(
					"授業時間が予約可能な時間帯の長さを超えています。");
		}
		
		// 個のリクエストでは同じ現在日時を使って判定する。
		LocalDateTime now = LocalDateTime.now(BOOKING_ZONE);
		
		List<ReservationSlot> slots = new ArrayList<>();
		
		// まず入力日時と、今回送信された候補同士をチェックする
		for (LocalDateTime startsAt : requestedStarts) {
			
			checkStart(startsAt, now);
			
			// 開始時間を分に変換
			int startMinutes = startsAt.getHour() * 60 + startsAt.getMinute();
			
			if (startMinutes < OPEN_MINUTES || startMinutes + lessonMinutes > CLOSE_MINUTES) {
				
				throw new ReservationCheckException(
						startsAt
						+ ": 授業全体が" + OPEN_MINUTES / 60 + ":00=" + CLOSE_MINUTES / 60 + ":00に収まるように選択してください");
			}
			
			LocalDateTime endsAt = startsAt.plusMinutes(lessonMinutes);
			
			// slotsに登録された他の登録候補との重複チェック
			// 一週目はslotsは空なので実行されない。上のネストのfor文の2週目から実行される
			// slotsへ値を入れるのはこのfor文が終わった後。
			for (ReservationSlot previous : slots) {
				
				if (overlaps(
						startsAt,
						endsAt,
						previous.getStartsAt(),
						previous.getEndsAt())) {
					
					throw new ReservationCheckException(
							startsAt
							+ ": 今回選択したほかの候補日と授業時間が重複しています。");
				}
			}
			
			slots.add(new ReservationSlot(startsAt, endsAt));
		}
		
		// 同じ日のデータを、個のリクエスト内で何度も取得しないためのMap
		
		// 受付可能時間のMap
		Map<LocalDate, List<LessonOpening>> openingsByDate = new HashMap<>();
		// 既存予約のMap
		Map<LocalDate, List<Reservation>> reservationsByDate = new HashMap<>();
		
		// DBの受付可能時間・既存予約と照合する
		for (ReservationSlot slot: slots) {
			
			LocalDate date = slot.getStartsAt().toLocalDate();
			
			// openingsに対応するキー（date）がなければgetOpeningを実行しMapに保存する
			List<LessonOpening> openings =
					openingsByDate.computeIfAbsent(
							date,
							reservationDataService::getOpening);
			
			// 受付可能時間内に収まる予約かチェック
			if (!isCoveredByOpenings(slot, openings)) {
				
				throw new ReservationCheckException(
						slot.getStartsAt()
						+ ": 受付可能時間ではない時間帯が含まれています。");
			}
			
			// confirmedReservationsに対応するキー（date）がなければgetOpeningを実行しMapに保存する
			List<Reservation> confirmedReservations =
					reservationsByDate.computeIfAbsent(
							date,
							reservationDataService::getConfirmedReservation);
			
			// 他の予約と重複していないかチェック
			for (Reservation reservation : confirmedReservations) {
				if (overlaps(
						slot.getStartsAt(),
						slot.getEndsAt(),
						reservation.getStartsAt(),
						reservation.getEndsAt())) {
					
					throw new ReservationCheckException(
							slot.getStartsAt()
							+ ": 指定した時間にはすでに別の予約が入っています。");
				}
			}
		}
		
		return List.copyOf(slots);
	}
	
	/** 開始日時そのものを検証する */
	private void checkStart(
			LocalDateTime startsAt,
			LocalDateTime now) {
		
		if (startsAt == null) {
			throw new ReservationCheckException(
					"開始日時が入力されていません。");
		}
		
		if (startsAt.getMinute() % STEP_MINUTES != 0
				|| startsAt.getSecond() != 0
				|| startsAt.getNano() != 0) {
			
			throw new ReservationCheckException(
					startsAt
					+ ": 開始時刻は30分刻みで選択してください。");
		}
		
		if (!startsAt.isAfter(now)) {
			throw new ReservationCheckException(
					startsAt
					+ ": 予約期限を過ぎた日時は予約できません。");
		}
	}
	
	/** 2つの時間帯が重なるかを判定する */
	private boolean overlaps(
			LocalDateTime startA,
			LocalDateTime endA,
			LocalDateTime startB,
			LocalDateTime endB) {
		
		return startA.isBefore(endB) && startB.isBefore(endA);
	}
	
	/**
	 * 授業全体が受付可能時間で覆われているかを判定する。
	 * 隣接・重複した受付枠は連続した時間として扱う。
	 */
	private boolean isCoveredByOpenings(
			ReservationSlot slot,
			List<LessonOpening> openings) {
		
		// 受付可能時間のリストopeningsを「開始日時が早い順」に並べ、新しいリストsortedに入れる処理
		List<LessonOpening> sorted = openings.stream()
				.sorted(Comparator.comparing(LessonOpening::getStartsAt))
				.toList();
		
		// 授業開始から、どこまで連続して受付可能か
		LocalDateTime coveredUntil = slot.getStartsAt();
		
		for (LessonOpening opening: sorted) {
			
			// 確認済みの位置より前に終わる枠は読み飛ばす
			if (!opening.getEndsAt().isAfter(coveredUntil)) {
				continue;
			}
			
			// 次の枠の開始までに隙間がある
			if (opening.getStartsAt().isAfter(coveredUntil)) {
				return false;
			}
			
			// 連続して受付可能な位置を伸ばす
			coveredUntil = opening.getEndsAt();
			
			if (!coveredUntil.isBefore(slot.getEndsAt())) {
				return true;
			}
		}
		
		return false;
	}
}
