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
	// ThymeleafがHTMLに設定したメッセージを取得
	const messages = byId('reservation-messages').dataset;
	// {0},{1}などの部分へ値を埋め込む
	function formatMessage(template, ...args) {
		
		return template.replace(
			/\{(\d+)\}/g,
			(matched, index) =>
				args[Number(index)] ?? matched
		);
	}
	const dialog = byId('time-dialog');
	const form = byId('reservation-form');
	const slotStarts = Array.from({length: (R.CLOSE - R.OPEN) / R.STEP}, (_, i) => R.OPEN + i * R.STEP);
	
	// 受付可能状況に関するデータを扱う
	const availability = ReservationCalendarData.create(
			app.dataset.calendarUrl
	);

	function dailyData(date) {
		return availability.dailyData(date);
	}

	const initialToday = R.todayKey();
	const state = {
		year: Number(initialToday.slice(0, 4)),
		month: Number(initialToday.slice(5, 7)) - 1,
		activeDate: null,
		candidates: []
	};
	let isSubmitting = false; // 予約送信中かどうかを管理する
	const displayLocale = app.dataset.locale || 'ja-JP';
	const dateFormatter = new Intl.DateTimeFormat(
		displayLocale, 
		{
			timeZone: 'UTC',
			year: 'numeric',
			month: 'long',
			day: 'numeric',
			weekday: 'short'
		}
	);
	const dateLabel = date => dateFormatter.format(new Date(`${date}T00:00:00Z`));
	const rangeLabel = item => `${R.timeText(item.start)}-${R.timeText(item.end)}`;
	const onDate = date => state.candidates.filter(item => item.date === date);
	const candidateId = (date, start) => `${date}_${start}`;
	
	function reasonAt(date, start, candidates = state.candidates) {
		
		if (!availability.isLoaded(date)) {
			return '空き状況を取得できません';
		}
		
		return R.startReason({date, start, duration: lessonMinutes, ...dailyData(date), candidates});
	}
	
	function element(tag, className, text) {
		const node = document.createElement(tag);
		if (className) node.className = className;
		if (text !== undefined) node.textContent = text;
		return node;
	}
	
	let calendarLoading = false;
	let calendarError = false;
	
	// 画面に表示している年月
	function visibleMonth() {
		
		return `${state.year}-${String(state.month + 1).padStart(2, '0')}`;
	}
	
	// 表示中の月のDBデータを取得する
	async function loadVisibleMonth() {
		
		if (calendarLoading) return;
		
		calendarLoading = true;
		calendarError = false;
		
		renderCalendar();
		
		try {
			await availability.loadMonth(visibleMonth());
		
		} catch (error) {
			
			calendarError = true;
			console.error(error);
			
		} finally {
			
			calendarLoading = false;
			
			renderCalendar();
			renderCandidates();
			
			if (dialog.open) {
				renderSlots();
			}
		}
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
					const ready = availability.isLoaded(key) && !calendarLoading;
					button.disabled = key < today || !ready;
					const count = onDate(key).length;
					const hasOpenings = !button.disabled && slotStarts.some(start => reasonAt(key, start) === null);
					
					let status;
					if (key < today) {
						status = '-';
					} else if (!ready) {
						status = calendarError ? '取得失敗' : '読み込み中';
					} else if (count > 0) {
						status = `${count}件選択`;
					} else {
						status = hasOpenings ? '空きあり' : '空きなし';
					}
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
	
	// ブラウザの履歴から戻った場合などに送信状態を解除する
	window.addEventListener('pageshow', () => {
		setSubmitting(false);
		// ボタンとhidden入力を現在の候補から再構築する
		renderCandidates();
	})
	
	// 予約失敗後、サーバから返された候補を復元する
	function restoreCandidates() {
		
		const restoredInput = byId('restored-starts');
		
		if (!restoredInput || !restoredInput.value) {
			return;
		}
		
		// カンマ区切りの日時をリストにする
		const restoredStarts = restoredInput.value.split(',');
		
		// 同じ日時を重複して復元しないためのSet
		const restoredIds = new Set();
		
		for (const value of restoredStarts) {
			
			const match = value.trim().match(
				/^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/
			);
			
			// 日時形式が不正な場合は無視する
			if (!match) continue;
			
			const date = match[1]
			const hour = Number(match[2]);
			const minute = Number(match[3]);
			const second = Number(match[4] ?? '0');
			
			// 時刻を分へ変換
			const start = hour * 60 + minute;
			const end = start + lessonMinutes;
			
			// 画面の基本的な時間条件をチェック
			if (second != 0
					|| start % R.STEP !== 0
					|| start < R.OPEN
					|| end > R.CLOSE) {
				
				continue;
			}
			
			// 日本時間で開始時刻が過去なら復元しない
			const startsAtMillis = Date.parse(
				`${date}T${R.timeText(start)}:00+09:00`
			);
			
			if (!Number.isFinite(startsAtMillis)
				|| startsAtMillis <= Date.now()) {
					continue;
			}
			
			const id = candidateId(date, start);
			
			if (restoredIds.has(id)) {
				continue;
			}
			
			restoredIds.add(id);
			
			// 既存の候補と同じ形式で保存
			state.candidates.push({
				date: date,
				start: start,
				end: end
			});
		}
		
		// 復元候補の最初の日付が属する月を表示する
		if (state.candidates.length > 0) {
			
			const firstDate = state.candidates
					.map(item => item.date)
					.sort()[0];
					
			state.year = Number(firstDate.slice(0, 4));
			state.month = Number(firstDate.slice(5, 7)) - 1;
		}
	}
	
	// 予約ボタンの有効・無効と表示を更新する
	function updateSubmitButton() {
		
		const button = byId('reserve-button');
		const count = state.candidates.length;
		
		const allReady =state.candidates.every(
			item => availability.isLoaded(item.date)
		);
		
		// 送信中、または候補が0件なら操作不可
		button.disabled = isSubmitting || count === 0 || !allReady;
		
		if (isSubmitting) {
			
			button.textContent = messages.buttonSubmitting;
			
		} else if (count > 0) {
			
			button.textContent = formatMessage(messages.buttonCount, count);
			
		} else {
			
			button.textContent = messages.buttonEmpty;
			
		}
		
		// CSSで送信中の見た目を変更するためのクラス
		button.classList.toggle('is-submitting', isSubmitting);
	}
	
	// 予約送信中の状態を変更する
	function setSubmitting(submitting) {
		
		isSubmitting = submitting;
		
		const feedback = byId('submit-feedback');
		
		if (isSubmitting) {
			
			feedback.textContent = messages.feedbackSubmitting;
			feedback.dataset.status = 'submitting';
			feedback.hidden = false;
			
		} else if (feedback.dataset.status === 'submitting') {
			
			// 送信中メッセージだけを削除する
			feedback.textContent = '';
			feedback.hidden = true;
			
			delete feedback.dataset.status;
		}
		
		updateSubmitButton();
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
			const remove = element('button', 'remove-button', messages.buttonRemove);
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
		byId('reservation-total').textContent = formatMessage(messages.summaryTotal, count, count * lessonMinutes);
		updateSubmitButton();
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
		if (isSubmitting || calendarLoading) return;
		const target = new Date(Date.UTC(state.year, state.month + offset, 1));
		if (R.keyFromDate(target).slice(0, 7) < R.todayKey().slice(0, 7)) return;
		state.year = target.getUTCFullYear();
		state.month = target.getUTCMonth();
		loadVisibleMonth();
	}
	
	// 予約フォームの送信処理
	form.addEventListener('submit', event => {
		
		// 既に送信中なら再送信を防ぐ
		if (isSubmitting) {
			event.preventDefault();
			return;
		}
		
		const feedback = byId('submit-feedback');
		feedback.hidden = true;
		feedback.textContent = '';
		
		if (state.candidates.length === 0) {
			event.preventDefault();
			
			feedback.textContent = messages.feedbackEmpty;
			feedback.hidden = false;
			return;
		}
		
		// 既存の予約日時チェック
		const invalid = state.candidates.find(item => 
			reasonAt(
				item.date,
				item.start,
				state.candidates.filter(other => other !== item)
			) !== null
		);
		// 予約候補に問題があれば送信しない
		if (invalid) {
			event.preventDefault();
			feedback.textContent = messages.feedbackInvalid;
			feedback.hidden = false;
			return;
		}
		
		// 入力に問題がなければ送信中に切り替える
		setSubmitting(true);
		
		//  preventDefault()を呼ばず通常のPOSTを行う
	});
	
	if (!R.isValidDuration(lessonMinutes) || typeof dialog.showModal !== 'function') {
		const error = byId('configuration-error');
		error.textContent = !R.isValidDuration(lessonMinutes)
			? messages.configDuration
			: messages.configBrowser;
			error.hidden = false;
			return;
	}
	
	app.querySelectorAll('[data-duration-label]').forEach(node => { node.textContent = String(lessonMinutes); });
	/* カレンダーの日付が押されたら予約可能な場合はモーダルを起動させる */
	byId('calendar-days').addEventListener('click', event => {
		if (isSubmitting) return;
		const button = event.target.closest('button[data-date]'); // dataset.dateがある場合のみ実行
		if (button && !button.disabled) openDate(button.dataset.date);
	})
	byId('previous-month').addEventListener('click', () => changeMonth(-1));
	byId('next-month').addEventListener('click', () => changeMonth(1));
	byId('current-month').addEventListener('click', () => {
		if (isSubmitting) return;
		const today = R.todayKey();
		state.year = Number(today.slice(0, 4));
		state.month = Number(today.slice(5, 7)) -1;
		renderCalendar();
	})
	
	byId('time-slots').addEventListener('click', event => {
		if (isSubmitting) return;
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
		if (isSubmitting) return;
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
	
	async function initializeCalendar() {
		
		restoreCandidates();
		renderCandidates();
		
		// エラー後に復元された候補が属する月も取得する
		const candedateMonths = new Set(
			state.candidates.map(item => item.date.slice(0, 7))
		);
		
		for (const month of candedateMonths) {
			
			if (month === visibleMonth()) continue;
			
			try {
				await availability.loadMonth(month);
			} catch (error) {
				console.error(error);
			}
		}
		
		// 初回表示月を取得する
		await loadVisibleMonth();
	}
	
	initializeCalendar();
	
})();