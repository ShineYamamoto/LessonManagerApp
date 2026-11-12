export const STEP_MINUTES = 30;
export const NEW_OPENING_MINUTES = 60;

export class OpeningDraftError extends Error {
	constructor(code, message) {
		super(message);
		this.name = 'OpeningDraftError';
		this.code = code;
	}
}

/** 有効な日付か */
export function isValidDate(value) {
	const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
	if (!match) return false;
	const [, yearText, monthText, dayText] = match;
	const year = Number(yearText), month = Number(monthText), day = Number(dayText);
	// うるう年か
	const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
	const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
	return year >= 1 && month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1];
}

/** LocalDateTimeを比較できる文字列に揃える */
export function normalizeLocalDateTime(value) {
	const match = /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/.exec(value);
	if (!match || !isValidDate(match[1])) throw new Error('受付枠の日時が不正です。');
	const [, date, hour, minute, second = '00', fraction = ''] = match;
	if (Number(hour) > 23 || Number(minute) > 59 ||  Number(second) > 59) {
		throw new Error('受付枠の日時が不正です。');
	}
	const significantFraction = fraction.replace(/0+$/, '');
	return `${date}T${hour}:${minute}:${second}${significantFraction ? '.' + significantFraction : ''}`;
}

export function formatMinutes(minutes) {
	return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
}

export function timeToMinutes(time) {
	const match = /^(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/.exec(time);
	if (!match || Number(match[1] > 23 || Number(match[2]) > 59 || Number(match[3] || 0) > 59)) {
		throw new Error('表示時間帯が不正です。');
	}
	return Number(match[1]) * 60 + Number(match[2]) + (Number(match[3] || 0) + Number('0.' + (match[4] || '0'))) / 60;
}

/** 端が接するだけなら重複しない。[開始, 終了)として比較する */
export function overlaps(first, second) {
	return first.startsAt < second.endsAt && second.startsAt < first.endsAt;
}

export function overlapsDate(opening, date) {
	return opening.startsAt < `${date}T24:00:00` && opening.endsAt > `${date}T00:00:00`;
}

/** 表示用に切りつめてもstate内の元の日時は書き換えない */
export function visibleRange(opening, date, displayStart, displayEnd) {
	if (!overlapsDate(opening, date)) return null;
	const start = opening.startsAt.slice(0, 10) < date ? 0 : timeToMinutes(opening.startsAt.slice(11));
	const end = opening.endsAt.slice(0, 10) > date ? 1440 : timeToMinutes(opening.endsAt.slice(11));
	const visibleStart = Math.max(start, displayStart);
	const visibleEnd = Math.min(end, displayEnd);
	if (visibleStart >= visibleEnd) return null;
	return {
		left: (visibleStart - displayStart) / (displayEnd - displayStart) * 100,
		width: (visibleEnd - visibleStart) / (displayEnd - displayStart) * 100,
		compact: visibleEnd - visibleStart < 60
	};
}

/** 一括保存でも利用できるよう追加・変更・削除を比較して取り出す */
export function calclulateChanges(originaolOpenings, draftOpenings) {
	const originalById = new Map(originaolOpenings.map(item => [item.openingId, item]));
	const remainingIds = new Set(draftOpenings.filter(item => item.openingId !== null)
				.map(item => item.openingId));
	const creates = draftOpenings.filter(item => item.openingId === null)
				.map(item => ({ ...item }));
	const updates = [];
	for (const draft of draftOpenings) {
		if (draft.openingId === null) continue;
		const original = originalById.get(draft.openingId);
		if (!original) throw new Error('比較基に存在しない受付枠です。');
		if (draft.startsAt != original.startsAt || draft.endsAt != original.endsAt) {
			updates.push({ ...draft, expectedVersion: original.expectedVersion });
		}
	}
	const deletes = originaolOpenings.filter(item => !remainingIds.has(item.openingId))
				.map(item => ({ ...item }));
	return { creates, updates, deletes };
}

/** 複数の受付枠を開始順に連結し、予約全体を隙間なく覆えるか調べる */
export function isCovered(reservation, openings) {
	const sorted = [ ...openings].sort((a, b) => a.startsAt.localeCompare(b.startsAt));
	let coveredUntil = reservation.startsAt; // 初期値
	for (const opening of sorted) {
		if (opening.endsAt <= coveredUntil) continue;
		if (opening.startsAt > coveredUntil) return false;
		coveredUntil = opening.endsAt;
		if (coveredUntil >= reservation.endsAt) return true;
	}
	return false;
}

export function createOpeningDraft(initialOpenings, options) {
	const { startMinutes, endMinutes } = options;
	const allowedDates = new Set(options.dates);
	if (![startMinutes, endMinutes].every(value => Number.isInteger(value) && value % STEP_MINUTES === 0)
		|| startMinutes < 0 || startMinutes >= endMinutes || endMinutes >= 1440) {
			throw new Error('表示時間帯の設定が不正です。');
		}
		const createClientId = options.createClientId || (() => `new-${crypto.randomUUID()}`);
		
		// 日付をまたぐ既存データが複数行に現れても、DBのIDで１件にまとめる
		const byId = new Map();
		for (const raw of initialOpenings) {
			const id = String(raw.openingId ?? '');
			const version = String(raw.expectedVersion ?? '');
			if (!/^[1-9]\d*$/.test(id) || !/^\d+$/.test(version)) throw new Error('受付枠のIDまたは更新番号が不正です。' + 'ID:' + id + 'version:' + version);
			const item = {
				clientId: `opening-${id}`, openingId: id, expectedVersion: version,
				startsAt: normalizeLocalDateTime(raw.startsAt), endsAt: normalizeLocalDateTime(raw.endsAt)
			};
			if (item.startsAt >= item.endsAt) throw new Error('受付枠の終了日時が不正です。');
			const previous = byId.get(id);
			if (previous && (previous.startsAt !== item.startsAt || previous.endsAt !== item.endsAt || previous.expectedVersion !== item.expectedVersion)) {
				throw new Error('同じ受付枠の情報が一致しません。画面を開きなおしてください。');
			}
			byId.set(id, item);
		}
		
		// 各要素もコピー・凍結する。元データと編集中データで同じオブジェクトを共有しない。
		const originalOpenings = Object.freeze([...byId.values()].map(item => Object.freeze({ ...item })));
		let draftOpenings = originalOpenings.map(item => ({ ...item }));
		
		// 予約は読み取り専用。表示範囲外の日時も保持し、受付行間の重複をIDで除去する
		const reservationById = new Map();
		if (!Array.isArray(options.reservations)) throw new Error('確定予約データがありません。');
		for (const raw of options.reservations) {
			const id = String(raw.reservationId ?? '');
			if (!/^[1-9]\d*$/.test(id)) throw new Error('確定予約のIDが不正です。');
			const item = Object.freeze({
				reservationId: id,
				startsAt: normalizeLocalDateTime(raw.startsAt),
				endsAt: normalizeLocalDateTime(raw.endsAt)
			});
			if (item.startsAt >= item.endsAt) throw new Error('確定予約の終了日時が不正です。');
			const previous = reservationById.get(id);
			if (previous && (previous.startsAt !== item.startsAt || previous.endsAt !== item.endsAt)) {
				throw new Error('同じ確定予約の情報が一致しません。');
			}
			reservationById.set(id, item);
		}
		const reservations = Object.freeze([...reservationById.values()]);
		
		function findOpening(clientId) {
			const item = draftOpenings.find(opening => opening.clientId === clientId);
			if (!item) throw new OpeningDraftError('notFound', '受付枠が見つかりません。');
			return item;
		}
		
		function getOpening(clientId) { return { ...findOpening(clientId) }; }
		
		// 古い範囲外データ・日をまたぐデータは、この画面では読み取り専用にする
		function canResize(clientId) {
			const item = findOpening(clientId);
			const date = item.startsAt.slice(0, 10);
			const start = timeToMinutes(item.startsAt.slice(11));
			const end = timeToMinutes(item.endsAt.slice(11));
			return allowedDates.has(date) && item.endsAt.slice(0, 10) === date
					&& Number.isInteger(start) && Number.isInteger(end)
					&& start % STEP_MINUTES === 0 && end % STEP_MINUTES === 0
					&& start >= startMinutes && end <= endMinutes
					&& end - start >= STEP_MINUTES;
		}
		
		function checkReservationPreserved(before, afterOpenings) {
			for (const reservation of reservations.filter(item => overlaps(item, before))) {
				if (!isCovered(reservation, afterOpenings)) {
					throw new OpeningDraftError(
						'reservationExists', 
						`確定予約 ${reservation.startsAt.replace('T', ' ')}～${reservation.endsAt.replace('T', ' ')}が受付時間の外に出ます。`
					);
				}
			}
		}
		
		// edgeはstart/endのどちらか
		function previewResize(clientId, edge, minute) {
			const before = findOpening(clientId);
			if (!canResize(clientId)) throw new OpeningDraftError('readOnly', 'この受付枠は表示のみです。');
			if (!['start', 'end'].includes(edge)) throw new OpeningDraftError('edge', '変更する端が不正です。');
			if (!Number.isInteger(minute) || minute % STEP_MINUTES !== 0) {
				throw new OpeningDraftError('step', '時刻は30分単位で指定してください。');
			}
			const date = before.startsAt.slice(0, 10);
			const start = edge === 'start' ? minute : timeToMinutes(before.startsAt.slice(11));
			const end = edge === 'end' ? minute : timeToMinutes(before.endsAt.slice(11));
			if (start < startMinutes || end > endMinutes) {
				throw new OpeningDraftError('outsideHours', '設定時間帯以外への変更はできません。');
			}
			if (end - start < STEP_MINUTES) {
				throw new OpeningDraftError('tooShort', '受付枠は最短30分です。');
			}
			const candidate = { ...before, startsAt: `${date}T${formatMinutes(start)}:00`, endsAt: `${date}T${formatMinutes(end)}:00` };
			if (draftOpenings.some(item => item.clientId !== clientId && overlaps(item, candidate))) {
				throw new OpeningDraftError('overlap', '他の受付枠と重なります。');
			};
			// 同じ時刻を選びなおすだけなら、変更を発生させない。
			if (candidate.startsAt !== before.startsAt || candidate.endsAt !== before.endsAt) {
				const after = draftOpenings.map(item => item.clientId === clientId ? candidate : item);
				checkReservationPreserved(before, after);
			}
			return candidate;
		}
		
		function resize(clientId, edge, minute) {
			// pointerup時も再検証する。失敗した場合はdraftを一切変更しない
			const candidate = previewResize(clientId, edge, minute);
			draftOpenings = draftOpenings.map(item => item.clientId === clientId ? candidate : item);
			return { ...candidate };
		}
		
		function candidate(date, start) {
			if (!allowedDates.has(date) || !isValidDate(date)) {
				throw new OpeningDraftError('date', '表示中の月の日付を選択してください。');
			}
			if (!Number.isInteger(start) || start % STEP_MINUTES !== 0) {
				throw new OpeningDraftError('step', '開始時刻は30分単位で選択してください。');
			}
			const end = start + NEW_OPENING_MINUTES;
			if (start < startMinutes || end > endMinutes) {
				throw new OpeningDraftError('outsideHours', 'この位置からは1時間の受付枠を追加できません。');
			}
			const result = { startsAt: `${date}T${formatMinutes(start)}:00`, endsAt: `${date}T${formatMinutes(end)}:00` };
			if (draftOpenings.some(item => overlaps(result, item))) {
				throw new OpeningDraftError('overlap', '既存の受付枠、または仮追加した受付枠と重なります。');
			}
			return result;
		}
		
		function add(date, start) {
			// 確認表示時だけでなく、「追加する」を押した時点でも再確認する
			const range = candidate(date, start);
			const clientId = createClientId();
			if (!clientId || draftOpenings.some(item => item.clientId === clientId)) {
				throw new Error('仮枠の識別子を作成できませんでした。');
			}
			const item = { clientId, openingId: null, expectedVersion: null, ...range };
			draftOpenings.push(item);
			return { ...item };
		}
		
		function remove(clientId) {
			const before = findOpening(clientId);
			
			// 現在の「表示のみ」の扱いを維持する
			if (!canResize(clientId)) {
				throw new OpeningDraftError(
					'readOnly', 'この授業は表示のみのため、この画面では削除できません。'
				);
			}
			
			// まだdraftOpeningsには代入しない
			const after = draftOpenings.filter(item => item.clientId !== clientId);
			
			// 削除した後も確定予約全体を覆えるか
			checkReservationPreserved(before, after);
			
			draftOpenings = after;
			
			return { ...before };
		}
		
		function reset() {
			draftOpenings = originalOpenings.map(item => ({ ...item }));
		}
		
		function getChanges() { return calclulateChanges(originalOpenings, draftOpenings); }
		function hasChanges() {
			const changes = getChanges();
			return changes.creates.length + changes.updates.length + changes.deletes.length > 0;
		}
		
		return {
			getOriginalOpenings: () => originalOpenings,
			getDraftOpenings: () => draftOpenings.map(item => ({ ...item })),
			candidate, add, remove, reset, getChanges, hasChanges,
			getOpening, canResize, previewResize, resize
		};
		
}