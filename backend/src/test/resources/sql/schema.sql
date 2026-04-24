CREATE TABLE IF NOT EXISTS `subject_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL,
    `parent_id` BIGINT NOT NULL DEFAULT 0,
    `level` TINYINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `error_type_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) DEFAULT '',
    `avatar` VARCHAR(255) DEFAULT '',
    `exam_type` VARCHAR(20) DEFAULT '',
    `target_score` INT DEFAULT 0,
    `role` VARCHAR(10) DEFAULT 'USER',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `subject` VARCHAR(20) NOT NULL,
    `module` VARCHAR(30) NOT NULL,
    `knowledge_point` VARCHAR(50) DEFAULT '',
    `type` VARCHAR(10) NOT NULL DEFAULT 'SINGLE',
    `difficulty` TINYINT NOT NULL DEFAULT 3,
    `content` TEXT NOT NULL,
    `options` TEXT,
    `answer` VARCHAR(10) NOT NULL,
    `analysis` TEXT,
    `source` VARCHAR(50) DEFAULT '',
    `frequency` VARCHAR(10) DEFAULT 'MEDIUM',
    `estimated_time` INT DEFAULT 60,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `practice_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` VARCHAR(10) NOT NULL,
    `is_correct` BOOLEAN NOT NULL,
    `time_spent` INT DEFAULT 0,
    `mode` VARCHAR(10) NOT NULL DEFAULT 'PRACTICE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `error_note` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `error_types` TEXT,
    `note` VARCHAR(200) DEFAULT '',
    `review_count` INT DEFAULT 0,
    `mastered` BOOLEAN DEFAULT FALSE,
    `next_review_at` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `collection` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `note` VARCHAR(500) DEFAULT '',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
