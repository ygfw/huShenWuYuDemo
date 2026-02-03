-- Huyao init.sql
-- Usage: mysql -u root -p < db/init.sql

CREATE DATABASE IF NOT EXISTS hushen
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE hushen;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  phonenumber VARCHAR(32),
  created_at DATETIME NOT NULL,
  level INT NOT NULL DEFAULT 1,
  draw_count INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS characters (
  id BIGINT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  image VARCHAR(255) NOT NULL,
  skill1desc TEXT,
  skill2desc TEXT,
  ultimate_desc TEXT,
  quote TEXT,
  obtained_at DATETIME
);

CREATE TABLE IF NOT EXISTS home_state (
  id BIGINT PRIMARY KEY,
  character_id BIGINT NOT NULL,
  skin_id BIGINT,
  quote TEXT
);

CREATE TABLE IF NOT EXISTS user_characters (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  character_id BIGINT NOT NULL,
  duplicate_count INT NOT NULL DEFAULT 0,
  obtained_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS character_skins (
  id BIGINT PRIMARY KEY,
  character_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  image VARCHAR(255) NOT NULL,
  skin_key VARCHAR(32) NOT NULL,
  is_default BOOLEAN NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS character_skills (
  id BIGINT PRIMARY KEY,
  character_id BIGINT NOT NULL,
  skill_key VARCHAR(32) NOT NULL,
  image VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS card_pools (
  id BIGINT PRIMARY KEY,
  pool_key VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  image VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL
);

-- Base characters (update image paths to match your files)
INSERT INTO characters (id, name, image, obtained_at)
VALUES
  (1, 'character1', '/images/character/character1.png', NOW()),
  (2, 'character2', '/images/character/character2.jpg', NOW()),
  (3, 'character3', '/images/character/character3.jpg', NOW()),
  (4, 'character4', '/images/character/character4.jpg', NOW()),
  (5, 'character5', '/images/character/character5.jpg', NOW()),
  (6, 'character6', '/images/character/character6.jpg', NOW()),
  (7, 'character7', '/images/character/character7.jpg', NOW()),
  (8, 'character8', '/images/character/character8.jpg', NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  image = VALUES(image),
  obtained_at = VALUES(obtained_at);

-- Default skins
INSERT INTO character_skins (id, character_id, name, image, skin_key, is_default, created_at)
VALUES
  (1001, 1, '原皮', '/images/character/character1.png', 'default', 1, NOW()),
  (1002, 2, '原皮', '/images/character/character2.jpg', 'default', 1, NOW()),
  (1003, 3, '原皮', '/images/character/character3.jpg', 'default', 1, NOW()),
  (1004, 4, '原皮', '/images/character/character4.jpg', 'default', 1, NOW()),
  (1005, 5, '原皮', '/images/character/character5.jpg', 'default', 1, NOW()),
  (1006, 6, '原皮', '/images/character/character6.jpg', 'default', 1, NOW()),
  (1007, 7, '原皮', '/images/character/character7.jpg', 'default', 1, NOW()),
  (1008, 8, '原皮', '/images/character/character8.jpg', 'default', 1, NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  image = VALUES(image),
  skin_key = VALUES(skin_key),
  is_default = VALUES(is_default);

-- Extra skin for character1
INSERT INTO character_skins (id, character_id, name, image, skin_key, is_default, created_at)
VALUES
  (1101, 1, '皮肤2', '/images/character_skins/character1_skin2.png', 'skin2', 0, NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  image = VALUES(image),
  skin_key = VALUES(skin_key),
  is_default = VALUES(is_default);

-- Skills for character1
INSERT INTO character_skills (id, character_id, skill_key, image, created_at)
VALUES
  (2001, 1, 'ultimate', '/images/skills/character1_bigSkill.png', NOW()),
  (2002, 1, 'skill1', '/images/skills/character1_skill1.png', NOW()),
  (2003, 1, 'skill2', '/images/skills/character1_skill2.png', NOW())
ON DUPLICATE KEY UPDATE
  image = VALUES(image);

-- Card pools
INSERT INTO card_pools (id, pool_key, name, image, created_at)
VALUES
  (3001, 'pool1', '卡池1', '/images/cardPool/cardPoolBackground1.png', NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  image = VALUES(image);

-- Home state defaults to character1 default skin
INSERT INTO home_state (id, character_id, skin_id, quote)
VALUES
  (1, 1, 1001, NULL)
ON DUPLICATE KEY UPDATE
  character_id = VALUES(character_id),
  skin_id = VALUES(skin_id);
