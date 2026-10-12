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
})

/* 画面のイベント処理 */
(() => {
	'use strict';
	const app = document.getElementById('booking-app');
	if (!app) return;
	
	function renderCalendar() {
		byId('month-label').textContent = `${state.year}年${state.month + 1}月`;
		const today = R.todayKey();
		console.log(today);
	}
	
	renderCalendar();
})();