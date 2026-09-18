/* 目標レベルマスタ */
CREATE TABLE IF NOT EXISTS goal_levels (
	id INT PRIMARY KEY
  , level_code VARCHAR(20) NOT NULL UNIQUE
  , display_order INT NOT NULL
  , enabled BOOLEAN NOT NULL DEFAULT TRUE
);

/* ユーザーマスタ */
CREATE TABLE IF NOT EXISTS users (
    e164_phone_number VARCHAR(50) PRIMARY KEY
  , user_name VARCHAR(50)
  , password VARCHAR(100)
  , goal_level_id INT
  , role VARCHAR(50)
  , CONSTRAINT fk_users_goal_level
  		FOREiGN KEY (goal_level_id)
  		REFERENCES goal_levels(id)
);
