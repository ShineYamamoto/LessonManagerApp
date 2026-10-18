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
import com.ShineYamamoto.LessonManagerApp.reservation.domain.exception.ReservationProblem;
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
	 * 
	 * 問題のある候補をすべて収集する
	 * 1件でも問題があればエラーを投げる
	 * 全件問題なければ予約枠の一覧を返す
	 */
	public List<ReservationSlot> check(
			Long userId,
			List<LocalDateTime> requestedStarts) {
		
		if (requestedStarts == null || requestedStarts.isEmpty()) {
			throw new ReservationCheckException(
					"予約希望日時を1件以上選択してください。");
		}
		
		// 授業時間はDBから取得する
		int lessonMinutes = reservationDataService.getLessonMinutes(userId);
		
		// 正の30分単位であることはDataServiceで検証済み
		if (lessonMinutes > CLOSE_MINUTES - OPEN_MINUTES) {
			throw new IllegalStateException(
					"授業時間が予約可能な時間帯の長さを超えています。");
		}
		
		// このリクエストでは同じ現在日時を使って判定する。
		LocalDateTime now = LocalDateTime.now(BOOKING_ZONE);
		
		// 条件を満たす予約候補
		List<ReservationSlot> validSlots = new ArrayList<>();
		
		// 条件を満たさない予約候補
		List<ReservationProblem> problems = new ArrayList<>();
		
		// 同じ日のデータを、個のリクエスト内で何度も取得しないためのMap
		
		// 受付可能時間のMap
		Map<LocalDate, List<LessonOpening>> openingsByDate = new HashMap<>();
		// 既存予約のMap
		Map<LocalDate, List<Reservation>> reservationsByDate = new HashMap<>();
		
		// 開始日時の早い順にチェックする
		List<LocalDateTime> sortedStarts = requestedStarts.stream()
					.sorted(Comparator.nullsFirst(
						Comparator.naturalOrder()))
					.toList();
		
		// まず入力日時と、今回送信された候補同士をチェックする
		for (LocalDateTime startsAt : sortedStarts) {
			
			// 開始日自体のチェック
			String startError = checkStartReason(startsAt, now, lessonMinutes);
			
			if (startError != null) {
				
				problems.add(
						new ReservationProblem(startsAt, startError));
				
				continue;
			}
			
			LocalDateTime endsAt = startsAt.plusMinutes(lessonMinutes);
			
			ReservationSlot slot = new ReservationSlot(startsAt, endsAt);
			
			LocalDate date = startsAt.toLocalDate();
			
			// 受付可能時間の取得
			// openingsByDateに対応するキー（date）がなければgetOpeningを実行しMapに保存する
			List<LessonOpening> openings = openingsByDate.computeIfAbsent(date, reservationDataService::getOpening);
			
			// 授業時間に収まっていない
			if (!isCoveredByOpenings(slot, openings)) {
				
				problems.add(new ReservationProblem(
						startsAt,
						"は受付可能時間外です。"
						+ "別の時間を選択してください。"));
				
				continue;
			}
			
			// 既存の確定予約を取得する
			// reservationsByDateに対応するキー（date）がなければgetOpeningを実行しMapに保存する
			List<Reservation> confirmedReservations = reservationsByDate.computeIfAbsent(date, reservationDataService::getConfirmedReservation);
			
			// 既存予約と重複するか
			boolean alreadyBooked = confirmedReservations.stream()
					.anyMatch(reservation ->
							overlaps(
									startsAt,
									endsAt,
									reservation.getStartsAt(),
									reservation.getEndsAt()
							)
					);
			
			if (alreadyBooked) {
				
				problems.add(new ReservationProblem(
						startsAt,
						"の時間は利用できなくなりました。"
						+ "別の時間を選択してください。"));
				
				continue;
			}
			
			
			// 今回slotsに登録された他の登録候補との重複チェック
			// 一週目はslotsは空なので実行されない。上のネストのfor文の2週目から実行される
			// slotsへ値を入れるのはこのfor文が終わった後。
			boolean overlapsCandidate = validSlots.stream()
					.anyMatch(previous ->
							overlaps(
									startsAt,
									endsAt,
									previous.getStartsAt(),
									previous.getEndsAt()
							)
					);
			
			if (overlapsCandidate) {
				
				problems.add(new ReservationProblem(
						startsAt,
						"は今回選択したほかの候補と重複しています。"
						+ "別の時間を選択してください。"));
				
				continue;
			}
			
			// すべての条件を満たした候補だけを追加する
			validSlots.add(slot);
		}
		
		// 1件でも問題があれば登録処理へ進まない
		if (!problems.isEmpty()) {
			
			throw new ReservationCheckException(problems);
		}
		
		// 全件問題がなかった場合のみ返す
		return List.copyOf(validSlots);
	}
	
	/** 開始日時そのものを検証する */
	private String checkStartReason(
			LocalDateTime startsAt,
			LocalDateTime now,
			int lessonMinutes) {
		
		if (startsAt == null) {
			return "開始日時が入力されていません。";
		}
		
		if (startsAt.getMinute() % STEP_MINUTES != 0
				|| startsAt.getSecond() != 0
				|| startsAt.getNano() != 0) {
			
			return "は30分刻みで選択してください。";
		}
		
		if (!startsAt.isAfter(now)) {
			return "は予約期限を過ぎています。日時の日時を選択してください";
		}
		
		int startMinutes = startsAt.getHour() * 60 + startsAt.getMinute();
		
		if (startMinutes < OPEN_MINUTES || startMinutes + lessonMinutes > CLOSE_MINUTES) {
			return "授業全体が" + OPEN_MINUTES / 60 + ":00～" + CLOSE_MINUTES / 60 + ":00に収まりません。"
					+ "別の時間を選択してください。";
		}
		
		return null;
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
