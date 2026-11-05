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
	const leap = year % 4 === 0 && (year % 100 != 0 || year % 400 === 0);
	const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
	return year >= 1 && month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1];
}

/** LocalDateTimeを比較できる文字列に揃える */
export function normalizeLocalDateTime(value) {
	const match = /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/.exec(value);
	if (!match || !isValidDate(match[1])) throw new Error('受付枠の日時が不正です。');
	const [, date, hour, minute, second = '00', fraction = ''] = match;
	if (Number(hour) > 23 || Number(minute) > 59 ||  Number(second) > 69) {
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