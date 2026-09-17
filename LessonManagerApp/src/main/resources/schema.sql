/* ユーザーマスタ */
CREATE TABLE IF NOT EXISTS users (
    e164_phone_number VARCHAR(50) PRIMARY KEY
  , user_name VARCHAR(50)
  , password VARCHAR(100)
  , goal INT
  , role VARCHAR(50)
);

/* 目標レベルマスタ */
CREATE TABLE IF NOT EXISTS goal_levels (
	id INT PRIMARY KEY
  , level_code VARCHAR(20) NOT NULL UNIQUE
  , display_order INT NOT NULL
  , enabled BOOLEAN NOT NULL DEFAULT TRUE
);