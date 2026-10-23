package com.ShineYamamoto.LessonManagerApp.reservation.domain.service;

import static com.ShineYamamoto.LessonManagerApp.reservation.domain.policy.ReservationPolicy.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
					"reservation.error.empty");
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
						"reservation.error.outsideOpening"));
				
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
						"reservation.error.booked"));
				
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
						"reservation.error.candidateOverlap"));
				
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
	
	/**
	 * 本人の予約の変更内容を検証する
	 * 問題なければ変更先の開始・終了日時を返す
	 * このメソッドではDBを更新しない
	 */
	public ReservationSlot checkChange(
			Long userId,
			Long reservationId,
			LocalDateTime requestedStart) {
		
		// 1回のチェックでは同じ現在日時を使用する
		LocalDateTime now = LocalDateTime.now(BOOKING_ZONE);
		
		// 開始前の確定予約をDBから取得する
		Reservation original = reservationDataService.getUpcomingReservation(
				userId,
				reservationId,
				now)
				.orElseThrow(() ->
						new ResponseStatusException(HttpStatus.NOT_FOUND, "対象の予約が見つかりません。"));
		
		// 元の予約の変更期限を確認する
		if (original.getStartsAt().isBefore(now.plusHours(CHANGE_DEADLINE_HOURS))) {
			
			throw new ReservationCheckException(
					"reservation.change.error.deadline");
		}
		
		// 授業時間は元の予約から計算する
		Duration duration = Duration.between(
				original.getStartsAt(),
				original.getEndsAt());
		
		long lessonMinutes = duration.toMinutes();
		
		// DBの授業時間が正確な30分単位であることを確認する
		if (lessonMinutes <= 0
				|| lessonMinutes % STEP_MINUTES != 0
				|| lessonMinutes > CLOSE_MINUTES - OPEN_MINUTES
				|| !duration.equals(Duration.ofMinutes(lessonMinutes))) {
			
			throw new IllegalStateException(
					"元の予約授業が不正です");
		}
		
		// 必須・30分刻み・過去日時・9：00～22：00を確認する
		String startError = checkStartReason(
				requestedStart,
				now,
				(int) lessonMinutes);
		
		if (startError != null) {
			throw new ReservationCheckException(startError);
		}
		
		// 変更先が現在から24時間以上か
		if (requestedStart.isBefore(now.plusHours(CHANGE_LEAD_HOURS))) {
			throw new ReservationCheckException("reservation.change.error.toosoon");
		}
		
		// 元と同じ開始日時への変更は受け付けない
		if (requestedStart.equals(original.getStartsAt())) {
			throw new ReservationCheckException("reservation.change.error.unchanged");
		}
		
		// 終了日時はサーバー側で計算する
		LocalDateTime requestedEnd = requestedStart.plusMinutes(lessonMinutes);
		
		ReservationSlot slot = new ReservationSlot(requestedStart, requestedEnd);
		
		LocalDate date = requestedStart.toLocalDate();
		
		// DBから受付可能時間を取得する
		List<LessonOpening> openings = reservationDataService.getOpening(date);
		
		// 授業全体が受付可能時間に収まっているか
		if (!isCoveredByOpenings(slot, openings)) {
			throw new ReservationCheckException("reservation.error.outsideOpening");
		}
		
		// 同じ日の確定予約を全ユーザー分取得する
		List<Reservation> confirmedReservations = reservationDataService.getConfirmedReservation(date);
		
		boolean alreadyBooked = confirmedReservations.stream()
				
				// 変更対象の予約1件だけを、重複判定から除外する
				.filter(reservation ->
						!reservation.getReservationId().equals(original.getReservationId()))
				.anyMatch(reservation ->
						overlaps(
								requestedStart,
								requestedEnd,
								reservation.getStartsAt(),
								reservation.getEndsAt()));
		
		if (alreadyBooked) {
			throw new ReservationCheckException("reservation.error.booked");
		}
		
		return slot;
		
	}
	
	/** 開始日時そのものを検証する */
	private String checkStartReason(
			LocalDateTime startsAt,
			LocalDateTime now,
			int lessonMinutes) {
		
		if (startsAt == null) {
			return "reservation.error.required";
		}
		
		if (startsAt.getMinute() % STEP_MINUTES != 0
				|| startsAt.getSecond() != 0
				|| startsAt.getNano() != 0) {
			
			return "reservation.error.invalidStep";
		}
		
		if (!startsAt.isAfter(now)) {
			return "reservation.error.expired";
		}
		
		int startMinutes = startsAt.getHour() * 60 + startsAt.getMinute();
		
		if (startMinutes < OPEN_MINUTES || startMinutes + lessonMinutes > CLOSE_MINUTES) {
			return "reservation.error.outsideHours";
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
