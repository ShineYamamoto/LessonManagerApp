/* ユーザーマスタ */
CREATE TABLE IF NOT EXISTS users (
	region_code VARCHAR(50) 
  , phone_number VARCHAR(50) PRIMARY KEY
  , user_name VARCHAR(50)
  , password VARCHAR(100)
  , goal INT
  , role VARCHAR(50)
);