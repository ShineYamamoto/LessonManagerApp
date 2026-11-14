package com.ShineYamamoto.LessonManagerApp.admin.session;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.ShineYamamoto.LessonManagerApp.reservation.view.LessonOpeningTimelineView;

/** 編集開始時のタイムラインデータを保存・管理するクラス */
@Component
@SessionScope
public class LessonOpeningTimelineEditStore {

	// 運用に合わせて変更
	private static final int MAX_EDITS = 10;
	private static final Duration RETENTION = Duration.ofHours(2);
	
	private final Map<String, Snapshot> snapshots = new LinkedHashMap<>();
	
	// 復元用レコード
	public record Snapshot(
			String editId,
			LessonOpeningTimelineView originalTimeline,
			Instant createdAt) {
		
	}
	
	public synchronized Snapshot open(
			LessonOpeningTimelineView timeline) {
		
		removeExpired();
		
		// 上限を超えないよう、古い編集情報から削除
		while (snapshots.size() >= MAX_EDITS) {
			String oldesiId = snapshots.keySet().iterator().next();
			snapshots.remove(oldesiId);
		}
		
		Snapshot snapshot = new Snapshot(
				UUID.randomUUID().toString(),
				timeline,
				Instant.now());
		
		snapshots.put(snapshot.editId(), snapshot);
		
		return snapshot;
	}
	
	public synchronized Snapshot find(String editId) {
		removeExpired();
		return snapshots.get(editId);
	}
	
	public synchronized void remove(String editId) {
		snapshots.remove(editId);
	}
	
	/** 保存期限が切れた編集情報を snapshots から削除する処理 */
	private void removeExpired() {
		Instant now = Instant.now();
		
		// 保持する有効期限を超えたものは削除する
		snapshots.values().removeIf(snapshot ->
				!snapshot.createdAt()
						.plus(RETENTION)
						.isAfter(now));
	}
}
