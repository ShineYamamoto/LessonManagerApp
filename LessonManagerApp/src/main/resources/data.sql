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