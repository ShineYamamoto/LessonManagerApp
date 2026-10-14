/* 予約処理や画面のイベント処理に必要なメソッド等の作成 */
const ReservationRules = (() => {
	const STEP = 30;
	const OPEN = 9 * 60;
	const CLOSE = 22 * 60;

	function timeText(minutes) {
		return String(Math.floor(minutes / 60)).padStart(2, '0') + ':' + String(minutes % 60).padStart(2, '0');
	}
	
	function isValidDuration(minutes) {
		return Number.isInteger(minutes) && minutes > 0 && minutes % STEP === 0 && minutes <= CLOSE - OPEN;
	}
	
	// 半区間 [開始, 終了]で判定する。13～14時と14～15時は重複しない
	function overlaps(startA, endA, startB, endB) {
		return startA < endB && startB < endA;
	}
	
	// 管理者が隣り合う受付枠を別々に持っていても、連続していれば1区画として扱う。
	function mergeRanges(ranges) {
		const sorted = ranges.map(range => [...range]).sort((a, b) => a[0] - b[0]);
		const merged = [];
		for (const [start, end] of sorted) {
			const last = merged[merged.length - 1];
			if (last && start <= last[1]) last[1] = Math.max(last[1], end);
			else merged.push([start, end]);
		}
		return merged;
	}
		
	function todayKey(now = Date.now()) {
		const parts = new Intl.DateTimeFormat('en-CA', {
			timeZone: 'Asia/Tokyo', year: 'numeric', month: '2-digit', day: '2-digit'
		}).formatToParts(new Date(now));
		const get = type => parts.find(part => part.type === type).value;
		return `${get('year')}-${get('month')}-${get('day')}`;
	}
	
	function keyFromDate(date) {
		return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')}`;
	}
	
	function localDateTime(date, minutes) {
		return `${date}T${timeText(minutes)}:00`;
	}
	
	// nullなら開始可能。開始の1枠だけでなく、終了まで全区間を検証する。	   
	function startReason({date, start, duration, openings, booked, candidates = [], now = Date.now()}) {
		const end = start + duration;
		if (!isValidDuration(duration)) return '授業時間エラー';
		if (start % STEP !== 0 || start < OPEN || end > CLOSE) return '授業時刻超過';
		if (Date.parse(localDateTime(date, start) + '+09:00') <= now) return '受付終了';
		const windows = mergeRanges(openings);
		const covers = (from, to) => windows.some(([a, b]) => a <= from && to <= b);
		if (!covers(start, start + STEP)) return '受付時間外';
		if (!covers(start, end)) return '連続枠なし';
		if (booked.some(([a, b]) => overlaps(start, start + STEP, a, b))) return '予約済み';
		if (booked.some(([a, b]) => overlaps(start, end, a, b))) return '予約と重複';
		if (candidates.some(item => item.date === date && overlaps(start, end, item.start, item.end))) return '候補と重複';
		return null;
	}
	
	return Object.freeze({STEP, OPEN, CLOSE, timeText, isValidDuration, overlaps, mergeRanges, todayKey, keyFromDate, localDateTime, startReason});
})();

/* 画面のイベント処理 */
(() => {
	'use strict';
	const app = document.getElementById('booking-app');
	if (!app) return;
	const R = ReservationRules;
	const lessonMinutes = Number(app.dataset.lessonMinutes);
	const byId = id => document.getElementById(id);
	const dialog = byId('time-dialog');
	const form = byId('reservation-form');
	const slotStarts = Array.from({length: (R.CLOSE - R.OPEN) / R.STEP}, (_, i) => R.OPEN + i * R.STEP);
	
	// 仮の管理者設定。分で記述。9*60=09:00, 13*60+30=13:30。
	// 日曜休み、水曜午後は14時から、土曜日は18時まで、その他は22時まで。
	const SAMPLE_SCHEDULE = {
		0: [],
		1: [[9 * 60, 12 * 60], [13 * 60, 22 * 60]],
		2: [[9 * 60, 12 * 60], [13 * 60, 22 * 60]],
		3: [[9 * 60, 12 * 60], [14 * 60, 20 * 60]],
		4: [[10 * 60, 12 * 60], [13 * 60, 22 * 60]],
		5: [[9 * 60, 12 * 60], [13 * 60, 22 * 60]],
		6: [[9 * 60, 12 * 60], [13 * 60, 18 * 60]]
	}
	// 日付固有の受付時間も設定できる。例： {'2026-11-03': []} は受付不可。]
	const DATE_OVERRIDES = {};
	
	function dailyData(date) {
		const weekday = new Date(`${date}T00:00:00Z`).getUTCDay();
		return {
			openings: DATE_OVERRIDES[date] ?? SAMPLE_SCHEDULE[weekday],
			// 他の予約だ既に入っている仮データ。候補日とは別に判定する。
			booked: weekday === 0 ? [] : [[10 * 60, 10 * 60 + 30], [15 * 60, 16 * 60]]
		}
	}

	const initialToday = R.todayKey();
	const state = {
		year: Number(initialToday.slice(0, 4)),
		month: Number(initialToday.slice(5, 7)) - 1,
		activeDate: null,
		candidates: []
	};
	const dateFormatter = new Intl.DateTimeFormat('ja-JP', {
		timeZone: 'UTC', year: 'numeric', month: 'long', day: 'numeric', weekday: 'short'
	});
	const dateLabel = date => dateFormatter.format(new Date(`${date}T00:00:00Z`));
	const rangeLabel = item => `${R.timeText(item.start)}-${R.timeText(item.end)}`;
	const onDate = date => state.candidates.filter(item => item.date === date);
	const candidateId = (date, start) => `${date}_${start}`;
	
	function reasonAt(date, start, candidates = state.candidates) {
		return R.startReason({date, start, duration: lessonMinutes, ...dailyData(date), candidates});
	}
	
	function element(tag, className, text) {
		const node = document.createElement(tag);
		if (className) node.className = className;
		if (text !== undefined) node.textContent = text;
		return node;
	}
	
	// カレンダーのベースを作成
	function renderCalendar() {
		byId('month-label').textContent = `${state.year}年${state.month + 1}月`;
		const today = R.todayKey();
		const firstKey = R.keyFromDate(new Date(Date.UTC(state.year, state.month, 1)));
		byId('previous-month').disabled = firstKey.slice(0, 7) <= today.slice(0, 7);
		const firstWeekday = new Date(Date.UTC(state.year, state.month, 1)).getUTCDay();
		const fragment = document.createDocumentFragment();
		
		// 常に6週表示にして、月を移動してもカードの高さが急に変わらないようにする
		for (let week = 0; week < 6; week++) {
			const row = document.createElement('tr');
			for (let day = 0; day < 7; day++) {
				const date= new Date(Date.UTC(state.year, state.month, 1 - firstWeekday + week * 7 + day));
				const key = R.keyFromDate(date);
				const isThisMonth = date.getUTCMonth() === state.month;
				const cell = document.createElement('td');
				if (!isThisMonth) {
					const filter = element('span', 'calendar-day outside');
					filter.setAttribute('aria-hidden', 'true');
					filter.append(element('span', 'day-number', String(date.getUTCDate())));
					cell.append(filter);
				} else {
					const button = element('button', 'calendar-day');
					button.type = 'button';
					button.dataset.date = key;
					button.disabled = key < today;
					const count = onDate(key).length;
					const hasOpenings = !button.disabled && slotStarts.some(start => reasonAt(key, start) === null);
					const status = count ? `${count}件選択` : button.disabled ? '-' : hasOpenings ? '空きあり' : '空きなし';
					button.classList.toggle('has-candidates', count > 0);
					button.classList.toggle('has-openings', hasOpenings);
					button.classList.toggle('is-today', key === today);
					if (key === today) button.setAttribute('aria-current', 'date');
					button.setAttribute('aria-label', `${dateLabel(key)}, ${status}。時間を選択`);
					button.setAttribute('aria-haspopup', 'dialog');
					button.setAttribute('aria-controls', 'time-dialog');
					button.append(element('span', 'day-number', String(date.getUTCDate())), element('span', 'day-status', status));
					cell.append(button);
				}
				row.append(cell);
			}
			fragment.append(row);
		}
		byId('calendar-days').replaceChildren(fragment);
	}
	
	// 予約を一覧にまとめる
	function renderCandidates() {
		state.candidates.sort((a, b) => a.date.localeCompare(b.date) || a.start - b.start);
		const list = document.createDocumentFragment();
		const fields = document.createDocumentFragment();
		state.candidates.forEach((item, index) => {
			const li = element('li', 'candidate-item');
			const content =document.createElement('div');
			const date = element('time', 'candidate-date', dateLabel(item.date));
			date.dateTime = item.date;
			content.append(date, element('span', 'candidate-time', rangeLabel(item)));
			const remove = element('button', 'remove-button', '取り消す');
			remove.type = 'button';
			remove.dataset.remove = candidateId(item.date, item.start);
			remove.setAttribute('aria-label', `${dateLabel(item.date)} ${rangeLabel(item)}の候補を取り消す`);
			li.append(content, remove);
			list.append(li);
			
			// 取り消し後も0,1,2...と連番にする。フォームにはユーザーIDを含めない。
			const input = document.createElement('input');
			input.type = 'hidden';
			input.name = `reservationList[${index}].startsAt`;
			input.value = R.localDateTime(item.date, item.start);
			fields.append(input);

		});
		byId('candidate-list').replaceChildren(list);
		byId('reservation-inputs').replaceChildren(fields);
		const count = state.candidates.length;
		byId('empty-candidates').hidden = count !== 0;
		byId('candidate-count').textContent = String(count);
		byId('reservation-total').textContent = `${count}回 / ${count * lessonMinutes}分`;
		byId('reserve-button').disabled = count === 0;
		byId('reserve-button').textContent = count ? `選択した${count}件を予約する` : '日時を選んでください';
	}
	
	// ダイアログの予約枠の作成
	function renderSlots() {
		if (!state.activeDate) return;
		const chosen = onDate(state.activeDate);
		const fragment = document.createDocumentFragment();
		let possibleStarts = 0;
		for (const start of slotStarts) {
			const existing = chosen.find(item => item.start <= start && start < item.end);
			const reason = existing ? null : reasonAt(state.activeDate, start);
			const slotReason = existing ? null : R.startReason({
				date: state.activeDate,
				start,
				duration: R.STEP,
				...dailyData(state.activeDate),
				candidates: state.candidates
			});
			const button = element('button', 'time-slot');
			button.type = 'button';
			button.dataset.start = String(start);
			// クリック可否は、授業時間分を確保できるかで決める
			button.disabled = !existing && reason !== null;
			// グレー表示は、この30分枠自体が利用できない場合だけ
			button.classList.toggle('is-unavailable', !existing && slotReason !== null);
			button.classList.toggle('is-selected', Boolean(existing));
			button.setAttribute('aria-pressed', String(Boolean(existing)));
			button.append(element('span', 'slot-time', `${R.timeText(start)}-${R.timeText(start+R.STEP)}`));
			const statusText = existing ? '✓ 選択済み' : slotReason !== null ? slotReason: reason !== null ? '空き・開始不可' : '開始可';
			button.append(element('span', 'slot-state', statusText));
			const label = existing
				? `${rangeLabel(existing)}の授業予約を選択済み。この授業を取り消す`
				: reason
					? `${R.timeText(start)}開始は選択不可。${reason}`
					: `${R.timeText(start)}開始、${R.timeText(start + lessonMinutes)}終了の授業を候補に追加`;
			button.setAttribute('aria-label', label);
			button.title = label;
			fragment.append(button);
			if (!existing && reason ===null) possibleStarts++;
		}
		byId('time-slots').replaceChildren(fragment);
		byId('no-start-times').hidden = possibleStarts !== 0;
		byId('day-selection-label').textContent = `この日の予約候補：${chosen.length}件`;
		byId('day-selection-times').textContent = chosen.length ? chosen.map(rangeLabel).join(' / ') : 'まだ選択していません';
	}
	
	function refresh(message = '') {
		renderCandidates();
		renderCalendar();
		if (dialog.open) renderSlots();
		byId('submit-feedback').hidden = true;
		byId('page-status').textContent = message;
		byId('dialog-status').textContent = message;
	}
	
	function openDate(date) {
		if (date < R.todayKey()) return;
		state.activeDate = date;
		byId('dialog-title').textContent = dateLabel(date);
		byId('dialog-status').textContent = '選択日時は予約候補に自動で反映されます。';
		renderSlots();
		dialog.showModal();
		// 時間一覧の前に、日付と説明から読める位置へフォーカスする
		byId('dialog-title').focus();
		dialog.querySelector('.dialog-body').scrollTop = 0;
	}
	
	/* 表示カレンダーの月を切り替える */
	function changeMonth(offset) {
		const target = new Date(Date.UTC(state.year, state.month + offset, 1));
		if (R.keyFromDate(target).slice(0, 7) < R.todayKey().slice(0, 7)) return;
		state.year = target.getUTCFullYear();
		state.month = target.getUTCMonth();
		renderCalendar();
	}
	
	// サンプルは必ず送信を止める。
	/*
	form.addEventListener('submit', event => {
		event.preventDefault();
		if (state.candidates.length === 0) return;
		const invalid = state.candidates.find(item => reasonAt(item.date, item.start, state.candidates.filter(other => other !== item)) !== null);
		const feedback = byId('submit-feedback');
		feedback.textContent = invalid
			? '開始時刻を過ぎた候補などが含まれています。日時を確認し選びなおしてください。'
			: `サンプルのため送信していません。選択中の${state.candidates.length}件をまとめて予約する操作を確認しました。`;
		feedback.hidden = false;
	})*/
	// サーバーへ送信。（入力に問題があれば送信を止める）
	form.addEventListener('submit', event => {
		const feedback = byId('submit-feedback');
		feedback.hidden = true;
		
		if (state.candidates.length === 0) {
			event.preventDefault();
			
			feedback.textContent = '予約希望日時を１件以上選択してください。';
			feedback.hidden = false;
			return;
		}
		
		const invalid = state.candidates.find(item => 
			reasonAt(
				item.date,
				item.start,
				state.candidates.filter(other => other !== null)
			) !== null
		);
		
		if (invalid) {
			event.preventDefault();
			console.log(state.candidates);
			console.log(invalid);
			feedback.textContent = 
				'開始時刻を過ぎた候補などが含まれています。'
				+ '日時を確認し選びなおしてください。';
			feedback.hidden = false;
			return;
		}
		
		// 問題がなければpreventDefault()を呼ばない
		// ブラウザがformのactionへ通常のPOST送信を行う
	});
	
	if (!R.isValidDuration(lessonMinutes) || typeof dialog.showModal !== 'function') {
		const error = byId('configuration-error');
		error.textContent = !R.isValidDuration(lessonMinutes)
			? '授業時間は30分単位の整数で、選択可能な時間内の長さを設定してください。'
			: 'この画面を利用するには、dialogに対応する新しいブラウザが必要です。';
			error.hidden = false;
			return;
	}
	
	app.querySelectorAll('[data-duration-label]').forEach(node => { node.textContent = String(lessonMinutes); });
	/* カレンダーの日付が押されたら予約可能な場合はモーダルを起動させる */
	byId('calendar-days').addEventListener('click', event => {
		const button = event.target.closest('button[data-date]'); // dataset.dateがある場合のみ実行
		if (button && !button.disabled) openDate(button.dataset.date);
	})
	byId('previous-month').addEventListener('click', () => changeMonth(-1));
	byId('next-month').addEventListener('click', () => changeMonth(1));
	byId('current-month').addEventListener('click', () => {
		const today = R.todayKey();
		state.year = Number(today.slice(0, 4));
		state.month = Number(today.slice(5, 7)) -1;
		renderCalendar();
	})
	
	byId('time-slots').addEventListener('click', event => {
		const button = event.target.closest('button[data-start]');
		if (!button || button.disabled || !state.activeDate) return;
		const start = Number(button.dataset.start);
		const existing = onDate(state.activeDate).find(item => item.start <= start && start < item.end);
		if (existing) {
			state.candidates = state.candidates.filter(item => item !== existing);
			refresh(`${rangeLabel(existing)}の授業を候補から外しました。`);
		} else {
			// クリック時点でもう一度確認する。開いたまま時間が経過した場合も弾く。
			const reason = reasonAt(state.activeDate, start);
			if (reason !== null) {
				renderSlots();
				byId('dialog-status').textContent = `この時間は選択できません。${reason}。`;
				byId('dialog-title').focus();
				return;
			}
			const item = {date: state.activeDate, start, end: start + lessonMinutes};
			state.candidates.push(item);
			refresh(`${rangeLabel(item)}を予約候補に追加しました`);
		}
		// DOMを書き直した後も押した時間枠にキーボードフォーカスを戻す。
		const nextFocus = byId('time-slots').querySelector(`button[data-start="${start}"]`);
		if (nextFocus && !nextFocus.disabled) nextFocus.focus({preventScroll: true});
		else byId('dialog-title').focus();
	})
	
	byId('candidate-list').addEventListener('click', event => {
		const button = event.target.closest('button[data-remove]');
		if (!button) return;
		const index = state.candidates.findIndex(item => candidateId(item.date, item.start) === button.dataset.remove);
		if (index < 0) return;
		const [removed] = state.candidates.splice(index, 1);
		refresh(`${dateLabel(removed.date)} ${rangeLabel(removed)}の候補を取り消しました。`);
		const remaining = byId('candidate-list').querySelectorAll('button[data-remove]');
		const nextFocus = remaining[Math.min(index, remaining.length - 1)] || byId('calendar-days').querySelector('button:not(:disabled)');
		nextFocus?.focus({preventScroll: true});
	})
	
	// ダイアログを閉じる
	byId('close-dialog').addEventListener('click', () => dialog.close());
	byId('done-dialog').addEventListener('click', () => dialog.close());
	
	// Tab / Shift+Tabの端ではモーダル内を循環させる
	dialog.addEventListener('keydown', event => {
		if (event.key !== 'Tab') return;
		const buttons = dialog.querySelectorAll('button:not(:disabled)');
		const first = buttons[0];
		const last = buttons[buttons.length - 1];
		if (event.shiftKey && (document.activeElement === first || document.activeElement === byId('dialog-title'))) {
			event.preventDefault();
			last.focus();
		} else if (!event.shiftKey && document.activeElement === last) {
			event.preventDefault();
			first.focus();
		}
	})
	
	// Escapeキーも標準daialogが処理する。再描画された日付ボタンへフォーカスを復元する
	dialog.addEventListener('close', () => {
		const button =byId('calendar-days').querySelector(`button[data-date="${state.activeDate}"]`);
		button?.focus({preventScroll: true});
	});
	dialog.addEventListener('click', event => {
		if (event.target != dialog) return;
		const rect =dialog.getBoundingClientRect();
		if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) dialog.close();
	})
	
	renderCandidates();
	renderCalendar();
})();