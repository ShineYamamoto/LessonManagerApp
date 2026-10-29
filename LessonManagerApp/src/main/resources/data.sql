/* 目標レベルマスタ */
INSERT INTO goal_levels (
	id
  , level_code
  , display_order
  , enabled
) VALUES
	(1, 'N1', 1, TRUE)
  , (2, 'N2', 2, TRUE)
  , (3, 'N3', 3, TRUE)
  , (4, 'N4', 4, TRUE)
  , (5, 'N5', 5, TRUE)
 ;

/* ユーザーマスタ */
INSERT INTO users (
    e164_phone_number
  , user_name
  , password
  , goal_level_id
  , role
) VALUES
	('+817012345678', 'ユーザー1', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 1, 'ROLE_ADMIN')
  , ('+817000000001', 'ユーザー2', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 1, 'ROLE_GENERAL')
  , ('+817000000002', 'ユーザー3', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 2, 'ROLE_GENERAL')
  , ('+817000000003', 'ユーザー4', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 3, 'ROLE_GENERAL')
  , ('+817000000004', 'ユーザー5', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 1, 'ROLE_GENERAL')
  , ('+817000000005', 'ユーザー6', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 5, 'ROLE_GENERAL')
  , ('+817000000006', 'ユーザー7', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 1, 'ROLE_GENERAL')
  , ('+8613800000000', 'ユーザー8', '$2a$10$q6ySu8gBO3huPa7WkcObyul4eCaQMPknaE3NnIewBU8blOYIMa/9C', 2, 'ROLE_GENERAL')
;

/* ユーザー1の予約 */
INSERT INTO reservations (
	user_id
  , starts_at
  , ends_at
  , status
) SELECT
	user_id
  , '2026-12-01 10:00:00'
  , '2026-12-01 11:00:00'
  , 'CONFIRMED'
FROM users
WHERE e164_phone_number = '+8613800000000'
;

INSERT INTO reservations (
	user_id
  , starts_at
  , ends_at
  , status
) SELECT
	user_id
  , '2026-12-08 10:00:00'
  , '2026-12-08 11:00:00'
  , 'CONFIRMED'
FROM users
WHERE e164_phone_number = '+8613800000000'
;

/* 受付可能時間の確認用データ */
INSERT INTO lesson_openings (
	starts_at
  , ends_at
) VALUES
	('2026-11-10 09:00:00', '2026-11-10 14:00:00')
  , ('2026-11-11 11:00:00', '2026-11-11 20:00:00')
;

/* 予約ロック用の初期データ */
INSERT INTO reservation_lock (lock_id)
VALUES (1)
;

/* 予約の時間帯設定 */
INSERT INTO reservation_settings (
	settings_id
  , selectable_start_time
  , selectable_end_time
) VALUES (
	1
  , '09:00:00'
  , '22:00:00'
)