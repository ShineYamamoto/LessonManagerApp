/* ユーザーマスタ */
INSERT INTO users (
	region_code
  , phone_number
  , user_name
  , password
  , goal
  , role
) VALUES
	('+81', '07000000000', 'ユーザー1', 'pass1', 1, 'ROLE_ADMIN')
  , ('+81', '07000000001', 'ユーザー2', 'pass2', 1, 'ROLE_GENERAL')
;