/* ユーザーマスタ */
INSERT INTO users (
    e164_phone_number
  , user_name
  , password
  , goal
  , role
) VALUES
	('+817000000000', 'ユーザー1', 'pass1', 1, 'ROLE_ADMIN')
  , ('+817000000001', 'ユーザー2', 'pass2', 1, 'ROLE_GENERAL')
;

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