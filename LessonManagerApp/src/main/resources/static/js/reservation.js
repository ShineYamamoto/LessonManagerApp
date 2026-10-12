/* 予約処理や画面のイベント処理に必要なメソッド等の作成 */
const ReservationRules = (() => {
	const STEP = 30;
	const OPEN = 9 * 60;
	const CLOSE = 22 * 60;
	
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
	
	return Object.freeze({todayKey, keyFromDate});
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
	const slotStarts = Array.from({length: (R.CLOSE - R.OPEN) / R.STEP}, (_, i) => R.OPEN + i * R.STEP);
	
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
	const onDate = date => state.candidates.filter(item => item.date === date);
	
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
					button.setAttribute('aria-label', 'dialog');
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
	
	function openDate(date) {
		if (date < R.todayKey()) return;
		state.activeDate = date;
		byId('dialog-title').textContent = dateLabel(date);
		byId('dialog-status').textContent = '選択日時は予約候補に自動で反映されます。';
		dialog.showModal();
		// 時間一覧の前に、日付と説明から読める一へフォーカスする
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
	
	// ダイアログを閉じる
	byId('close-dialog').addEventListener('click', () => dialog.close());
	byId('done-dialog').addEventListener('click', () => dialog.close());
	
	renderCalendar();
})();