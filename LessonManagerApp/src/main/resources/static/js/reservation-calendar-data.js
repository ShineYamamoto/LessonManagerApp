const ReservationCalendarData = (() => {
	'use strict';
	
	function create(url) {
		
		//日付ごとの空き状況
		const days = new Map();
		
		// 取得済みの年月
		const loadedMonths = new Set();
		
		/** 指定付きのデータをサーバーから取得する */
		async function loadMonth(month) {
			
			loadedMonths.delete(month);
			
			const response = await fetch(
				`${url}?month=${encodeURIComponent(month)}`,
				{
					method: 'GET',
					credentials: 'same-origin',
					cache: 'no-store',
					headers: { 'Accept': 'application/json' }
				}
			);
			
			if (!response.ok) {
				throw new Error(`空き状況取得失敗: ${response.status}`);
			}
			
			const data = await response.json();
			
			// サーバーのJSONを既存JSの配列形式に変換
			for (const [date, value] of Object.entries(data)) {
				
				days.set(date, {
					// 受付可能時間をまとめる
					openings: value.openings.map(range => [range.start, range.end]),
					// 予約済み時間をまとめる
					booled: value.booked.map(range => [range.start, range.end])
				});
			}
			
			loadedMonths.add(month);
		}
		
		/** 指定日のデータを返す */
		function dailyData(date) {
			
			return days.get(date) ?? {
				openings: [],
				booked: []
			};
		}
		
		/** 指定付きの取得が完了しているか */
		function isLoaded(date) {
			
			return loadedMonths.has(date.slice(0, 7));
		}
		
		return Object.freeze({
			loadMonth,
			dailyData,
			isLoaded
		});
	}
	
	return Object.freeze({create});
})();