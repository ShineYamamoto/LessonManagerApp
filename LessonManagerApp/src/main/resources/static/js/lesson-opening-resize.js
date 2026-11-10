import { STEP_MINUTES, timeToMinutes, visibleRange } from './lesson-opening-draft.js';

/** クリックした端とポインターのずれを保ち、スクロール後も現在のトラック位置で計算する */
export function pointerToMinutes(clientX, rect, grabOffset, start, end) {
	if (!(rect.width > 0)) throw new Error('タイムラインの幅を取得できません。');
	const raw = start + (clientX - rect.left - grabOffset) / rect.width * (end - start);
	return Math.max(start, Math.min(end, Math.round(raw / STEP_MINUTES) * STEP_MINUTES));
}

export function installOpeningResize({ root, state, startMinutes, endMinutes, render, showMessage }) {
	let active = null;
	let suppressClick = false;
	
	function restoreFocus(clientId, edge) {
		const handle = [...root.querySelectorAll('.lot-resize-handle')].find(element =>
			element.dataset.edge === edge && element.closest('.lot-opening').dataset.clientId === clientId
		);
		handle?.focus({ preventScroll: true });
	}
	
	function paint(drag, candidate) {
		const range = visibleRange(candidate, drag.date, startMinutes, endMinutes);
		if (!range) return;
		drag.bar.style.left = `${range.left}%`;
		drag.bar.style.width = `${range.width}%`;
		drag.bar.classList.toggle('lot-bar-compact', range.compact);
		drag.bar.querySelector('.lot-start-label').textContent = candidate.startsAt.slice(11, 16);
		drag.bar.querySelector('.lot-end-label').textContent = candidate.endsAt.slice(11, 16);
	}
	
	function updatePreview(event) {
		const drag = active;
		if (!drag || event.pointerId !== drag.pointerId) return;
		event.preventDefault();
		// 端を押しただけ・上下に動かしただけでは時刻を変更しない
		if (!drag.moved && Math.abs(event.clientX - drag.initialX) < 4) return;
		drag.moved = true;
		const minute = pointerToMinutes(event.clientX, drag.track.getBoundingClientRect(), drag.grabOffset, startMinutes, endMinutes);
		if (minute === drag.minute) return;
		drag.minute = minute;
		try {
			const candidate = state.previewResize(drag.clientId, drag.edge, minute);
			paint(drag, candidate);
			drag.bar.classList.remove('lot-resize-invalid');
			showMessage(`${drag.date} ${candidate.startsAt.slice(11, 16)}～${candidate.endsAt.slice(11, 16)}：離すと変更します。`);
		} catch (error) {
			// 不正な候補をdraftへ入れない。赤枠にして理由を画面上に示す。
			drag.bar.classList.add('lot-resize-invalid');
			showMessage(error.message, true);
		}
	}
	
	function finish(commit) {
		const drag = active;
		if (!drag) return;
		// releasePointerCaptureで発生するLostPointercaptureの二重処理を防ぐ
		active = null;
		let message = '伸縮をキャンセルしました';
		let error = false;
		if (commit && drag.moved) {
			try {
				const candidate = state.resize(drag.clientId, drag.edge, drag.minute);
				const changed = candidate.startsAt !== drag.before.startsAt || candidate.endsAt !== drag.before.endsAt;
				message = changed ? '受付枠を変更しました。まだ保存されていません。' : '時刻の変更はありません。';
			} catch (cause) {
				message = `${cause.message} 伸縮前の時刻に戻りました。`;
				error = true;
			}
		} else if (commit) {
			message = '';
		}
		root.classList.remove('lot-is-resizing');
		if (drag.handle.hasPointerCapture(drag.pointerId)) drag.handle.releasePointerCapture(drag.pointerId);
		// pointerup直後のclickが書き直した下の空白セルを選ばないようにする
		suppressClick = true;
		window.setTimeout(() => { suppressClick = false; }, 0);
		render();
		showMessage(message, error);
		restoreFocus(drag.clientId, drag.edge);
	}
	
	root.addEventListener('pointerdown', event => {
		const handle = event.target.closest('.lot-resize-handle');
		if (!handle || active || !event.isPrimary || event.button !== 0) return;
		const bar = handle.closest('.lot-opening');
		const clientId = bar.dataset.clientId;
		if (!state.canResize(clientId)) return;
		const before = state.getOpening(clientId);
		const edge = handle.dataset.edge;
		const track = bar.closest('.lot-track');
		const rect = track.getBoundingClientRect();
		if (!(rect.width > 0)) return;
		const minute = timeToMinutes((edge === 'start' ? before.startsAt : before.endsAt).slice(11));
		const edgeX = rect.left + (minute - startMinutes) / (endMinutes - startMinutes) * rect.width;
		event.preventDefault();
		handle.focus({ preventScroll: true});
		active = { handle, bar, track, clientId, edge, before, date: track.dataset.date,
				pointerId: event.pointerId, initialX: event.clientX,
				grabOffset: event.clientX - edgeX, minute, moved: false
		};
		try {
			handle.setPointerCapture(event.pointerId);
			root.classList.add('lot-is-resizing');
			bar.classList.add('lot-resize-preview');
		} catch (error) {
			active = null;
			showMessage('変更をできませんでした。もう一度操作してください。', true);
		}
	});
	
	root.addEventListener('pointermove', updatePreview);
	root.addEventListener('pointerup', event => {
		if (!active || event.pointerId !== active.pointerId) return;
		updatePreview(event);
		finish(true);
	});
	root.addEventListener('pointercancel', event => {
		if (active && event.pointerId === active.pointerId) finish(false);
	});
	root.addEventListener('lostpointercapture', event => {
		if (active && event.pointerId == active.pointerId) finish(false);
	});
	window.addEventListener('blur', () => finish(false));
	window.addEventListener('resize', () => finish(false));
	
	root.addEventListener('click', event => {
		if (active || suppressClick || event.target.closest('.lot-resize-handle')) {
			event.preventDefault();
			event.stopImmediatePropagation();
		}
	}, true);
	
	// マウス以外でも編集できるよう、端のボタンに左右キー操作を用意する
	root.addEventListener('keydown', event => {
		if (active) {
			if (event.key === 'Escape') {
				event.preventDefault();
				event.stopPropagation();
				finish(false);
			} else if (event.key !== 'Tab') {
				event.preventDefault();
			} else {
				finish(false);
			}
			return;
		}
		const handle = event.target.closest('.lot-resize-handle');
		if (!handle || !['ArrowLeft', 'ArrowRight'].includes(event.key)	|| event.ctrlKey || event.metaKey || event.altKey) return;
		event.preventDefault();
		const clientId = handle.closest('.lot-opening').dataset.clientId;
		const edge = handle.dataset.edge;
		const before = state.getOpening(clientId);
		const minute = timeToMinutes((edge === 'start' ? before.startsAt : before.endsAt).slice(11)) + (event.key === 'ArrowRight' ? STEP_MINUTES : -STEP_MINUTES);
		try {
			state.resize(clientId, edge, minute);
			render();
			showMessage('受付枠を変更しました。まだ保存されていません。');
			restoreFocus(clientId, edge);
		} catch (error) {
			showMessage(error.message, true);
		}
	});
	return { isActive: () => active !== null, cancel: () => finish(false) };
}