import {
	STEP_MINUTES, OpeningDraftError, createOpeningDraft, formatMinutes, timeToMinutes, overlapsDate, visibleRange
} from './lesson-opening-draft.js';

const root = document.getElementById('lot-editor');
if (root) {
	try {
		initializeEditor(root);
	} catch (error) {
		console.error(error);
		const message = root.querySelector('#lot-editor-message');
		message.textContent = '仮追加を開始できませんでした。画面を開きなおしてください。';
		message.hidden = false;
	}
}

function initializeEditor(root) {
	const tracks = [...root.querySelectorAll('.lot-track[data-date]')];
	const startMinutes = timeToMinutes(root.dataset.startTime);
	const endMinutes = timeToMinutes(root.dataset.endTime);
	const seedElements = root.querySelectorAll('[data-opening-seed]');
	
	// datasetは文字列。LongのID/versionをNumberへ変換せず、そのまま保持する。
	const initialOpenings = [...seedElements].map(element => ({
		openingId: element.dataset.openingId,
		expectedVersion: element.dataset.version,
		startsAt: element.dataset.startsAt,
		endsAt: element.dataset.endsAt
	}));
	
	const state = createOpeningDraft(initialOpenings, {
		startMinutes, endMinutes, dates: tracks.map(track => track.dataset.date)
	});
	const dialog = root.querySelector("#lot-add-dialog");
	if (typeof dialog.showModal !== 'function') throw new Error('確認ダイアログを利用できません。');
	
	const confirmButton = root.querySelector('#lot-confirm-add');
	const message = root.querySelector('#lot-editor-message');
	const pendingSection = root.querySelector('#lot-pending');
	const pendingList = root.querySelector('#lot-pending-list');
	const resetButton = root.querySelector('#lot-reset-draft');
	const scroll = root.querySelector('.lot-scroll');
	
	let pendingCandidate = null;
	let leavingApproved = false;
	let unloadGuardEnabled = false;
	
	/** 画面を離れる直前に、ブラウザへ確認表示を要求する */
	function beforeUnload(event) {
		if (!state.hasChanges() || leavingApproved) return;
		event.preventDefault();
		event.returnValue = '';
	}
	
	/** 警告する処理を有効・無効にする */
	function syncUnloadGuard() {
		// 警告が必要か計算
		const needed = state.hasChanges() && !leavingApproved;
		if (needed === unloadGuardEnabled) return;
		if (needed) window.addEventListener('beforeunload', beforeUnload);
		else window.removeEventListener('beforeunload', beforeUnload);
		unloadGuardEnabled = needed;
	}
	
	function showMessage(text, isError = false) {
		message.textContent = text;
		message.hidden = !text;
		message.classList.toggle('lot-editor-error', isError);
	}
	
	function description(opening) {
		const kind = opening.openingId === null ? '仮追加' : '受付可能';
		return `${kind} ${opening.startsAt.replace('T', ' ')}～${opening.endsAt.replace('T', ' ')}`;
	}
	
	function makeBar(opening, date) {
		const range =visibleRange(opening, date, startMinutes, endMinutes);
		if (!range) return null;
		const element = document.createEelement('div');
		element.className = 'lot-opening';
		element.classList.toggle('lot-bar-compact', range.compact);
		element.classList.toggle('lot-opening-new', opening.openingId === null);
		element.dataset.clientId = opening.clientId;
		if (opening.openingId !== null) {
			element.dataset.openingId = opening.openingId;
			element.dataset.version = opening.expectedVersion;
		}
		element.style.left = `${range.left}%`;
		element.style.width = `${range.width}%`;
		element.setAttribute('role', 'img');
		element.setAttribute('aria-label', description(opening));
		element.title = description(opening);
		for (const [text, className] of [
			[opening.startsAt.slice(11, 16), ''],
			['-', 'lot-time-separator'],
			[opening.endsAt.slice(11, 16), '']
		]) {
			const span = document.createElement('span');
			span.textContent = text;
			span.className = className;
			span.setAttribute('aria-hidden', 'true');
			element.append(span);
		}
		if (opening.openingId === null) {
			const badge = document.createElement('small');
			badge.className = 'lot-draft-badge';
			badge.textContent = '仮';
			badge.setAttribute('aria-hidden', 'true');
			element.append(badge);
		}
		return element;
	}
	
	function renderCells(track) {
		let cells = track.querySelector('.lot-add-cells');
		if (!cells) {
			cells = document.createElement('div');
			cells.className = 'lot-add-cells';
			// 実際の30分セルを作るため、スクロール量からクリック時刻を逆算する必要はない
			for (let start = startMinutes; start < endMinutes; start += STEP_MINUTES) {
				const button = document.createElement('button');
				button.type = 'button';
				button.className = 'lot-add-cell';
				button.dataset.date = track.dataset.date;
				button.dataset.startMinutes = String(start);
				cells.append(button);
			}
			track.prepend(cells);
		}
		for (const button of cells.children) {
			const start = Number(button.dataset.startMinutes);
			let reason = '';
			try {
				state.candidate(track.dataset.date, start);
			} catch (error) {
				if (!(error instanceof OpeningDraftError)) throw error;
				reason = error.message;
			}
			const label = `${track.dataset.date} ${formatMinutes(start)}から1時間の受付枠を追加`;
			button.diabled = !!reason;
			button.title = reason || label;
			button.setAttribute('aria-label', reason ? `${label}:${reason}` : label);
		}
	}
	
	function render() {
		const draftOpenings = state.getDraftOpenings();
		const added = state.getChanges().creates;
		
		for (const track of tracks) {
			const date = track.dataset.date;
			const dayOpenings = draftOpenings.filter(item => overlapsDate(item, date));
			const layer = track.querySelector('.lot-openings-layer');
			const bars = dayOpenings.map(item => makeBar(item, date)).filter(Boolean);
			layer.replaceChildren(...bars);
			renderCells(track);
			const empty = track.querySelector('.lot-no-opening');
			empty.hidden = dayOpenings.length > 0 || Number(track.dataset.reservationCount) > 0;
		}
		
		// 日時一覧もdraftから更新し、画面のバート食い違わないようにする
		for (const section of root.querySelectorAll('.lot-detail-day[data-date]')) {
			const dayOpenings = draftOpenings.filter(item => overlapsDate(item, section.dataset.date));
			const list = section.querySelector('.lot-detail-openings');
			list.replaceChildren(...dayOpenings.map(item => {
				const li = document.createElement('li');
				li.textContent = description(item);
				return li;
			}));
			section.hidden = dayOpenings.length === 0 && Number(section.dataset.reservationCount === 0);
		}
		
		pendingList.replaceChildren(...added.map(item => {
			const li = document.createElement('li');
			const text = document.createElement('span');
			text.textContent = `${item.startsAt.slice(0, 10)} ${item.startsAt.clice(11, 16)}～${item.endsAt.slice(11, 16)}`;
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'lot-button lot-remove-draft';
			button.dataset.clientId = item.clientId;
			button.textContent = '取り消す';
			button.setAttribute('aria-label', `${text.textContent}の仮追加を取り消す`);
			li.append(text, button);
			return li;
		}));
		pendingSection.hidden = added.length === 0;
		resetButton.disabled = added.length === 0;
		root.querySelector('#lot-draft-status').textContent = added.length ? `未保存 ${added.length}件` : '変更なし';
		const reservationCount = Number(root.dataset.reservationCount);
		root.querySelector('#lot-counts').textContent = `受付枠 ${draftOpenings.length}件（仮追加 ${added.length}件) ・確定予約 ${reservationCount}件`;
		const emptyMonth = draftOpenings.length === 0 && reservationCount === 0;
		root.querySelector('#lot-empty-month').hidden = !emptyMonth;
		root.querySelector('#lot-empty-details').hidden = !emptyMonth;
		syncUnloadGuard();
	}
	
	root.addEventListener('click', event => {
		const cell = event.target.closest('.lot-add-cell');
		if (cell && !cell.disabled) {
			try {
				const date = cell.dataset.date;
				const start = Number(cell.dataset.startMinutes);
				const range = state.candidate(date, start);
				pendingCandidate = { date, start };
				root.querySelector('#lot-add-description').textContent = `${date} ${range.startsAt.slice(11, 16)}～${range.endsAt.slice(11, 16)}
						の受付枠を仮追加しますか？`;
				root.querySelector('#lot-add-error').hidden = true;
				confirmButton.disabled = false;
				dialog.showModal();
			} catch (error) {
				showMessage(error.message, true);
			}
		}
		const removeButton = event.target.closest('.lot-remove-draft');
		if (removeButton) {
			state.removeNew(removeButton.dataset.clientId);
			render();
			showMessage('仮追加を取り消しました。');
			scroll.focus({ preventScroll: true });
		}
	});
	
	root.querySelector('#lot-cancel-add').addEventListener('click', () => dialog.close());
	
	dialog.addEventListener('close', () => { pendingCandidate == null; });
	
	confirmButton.addEventListener('click', () => {
		if (!pendingCandidate || confirmButton.disabled) return;
		confirmButton.disabled = true;
		try {
			state.add(pendingCandidate.date, pendingCandidate.start);
			dialog.close();
			render();
			showMessage('受付枠を追加しました。まだ保存されていません。');
			scroll.focus({ preventScroll: true });
		} catch (error) {
			const errorElement = root.querySelector('#lot-add-error');
			errorElement.textContent = error.message;
			errorElement.hidden = false;
			confirmButton.disabled = false;
		}
	});
	
	resetButton.addEventListener('click', () => {
		if (!window,confirm('この画面で仮追加した受付枠をすべて取り消しますか？')) return;
		state.reset();
		render();
		showMessage('仮追加した受付枠をすべて取り消しました。');
		scroll.focus({ preventScroll: true });
	});
	
	function approveLeaving() {
		if (!state.hasChanges()) return true;
		if (!window.confirm('未保存の仮追加があります。破棄して移動しますか？')) return false;
		leavingApproved = true;
		syncUnloadGuard(); // 同じ異動でブラウザの警告を重ねて出さない
		return true;
	}
	
	// 月切り替えだけでなく共通メニューのリンクも対象。別タブで開く操作は現在の画面を失わない
	document.addEventListener('click', event => {
		const link = event.target.closest('a[href]');
		if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || link.hasAttribute('download') || (link.target && link.target !== '_self')) return;
		const url = new URL(link.href, location.href);
		if (!['http:', 'https:'].includes(url.protocol)) return;
		if (url.origin === location.origin && url.hash) return;
		if (!approveLeaving()) event.preventDefault();
	});
	
	document.addEventListener('submit', event => {
		const form = event.target;
		if (event.defaultPrevented || form.method === 'dialog' || (form.target && form.target !== '_self')) return;
		if (!approveLeaving()) event.preventDefault();
	});
	
	window.addEventListener('pageshow', () => {
		leavingApproved = false;
		syncUnloadGuard();
	});
	
	render();
	root.querySelectorAll('[data-editor-controls]').forEach(element => { element.hidden = false; });
}