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
	('+817000000000', 'ユーザー1', 'pass1', 1, 'ROLE_ADMIN')
  , ('+817000000001', 'ユーザー2', 'pass2', 1, 'ROLE_GENERAL')
  , ('+817000000002', 'ユーザー3', 'pass3', 2, 'ROLE_GENERAL')
  , ('+817000000003', 'ユーザー4', 'pass4', 3, 'ROLE_GENERAL')
  , ('+817000000004', 'ユーザー5', 'pass5', 1, 'ROLE_GENERAL')
  , ('+817000000005', 'ユーザー6', 'pass6', 5, 'ROLE_GENERAL')
  , ('+817000000006', 'ユーザー7', 'pass7', 1, 'ROLE_GENERAL')
  , ('+869012345678', 'ユーザー8', 'pass8', 2, 'ROLE_GENERAL')
;
