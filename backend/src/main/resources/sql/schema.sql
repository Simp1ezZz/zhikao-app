CREATE DATABASE IF NOT EXISTS zhikao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zhikao;

-- 科目/模块/知识点 配置表（树形结构）
CREATE TABLE `subject_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '名称',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID（0=顶级）',
    `level` TINYINT NOT NULL COMMENT '层级：1=科目,2=模块,3=知识点',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_level` (`level`)
);

-- 错因类型 配置表
CREATE TABLE `error_type_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '错因名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) DEFAULT '',
    `avatar` VARCHAR(255) DEFAULT '',
    `exam_type` VARCHAR(20) DEFAULT '',
    `target_score` INT DEFAULT 0,
    `role` VARCHAR(10) DEFAULT 'USER',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `subject` VARCHAR(20) NOT NULL COMMENT '科目（关联subject_config level=1）',
    `module` VARCHAR(30) NOT NULL COMMENT '模块（关联subject_config level=2）',
    `knowledge_point` VARCHAR(50) DEFAULT '' COMMENT '知识点（关联subject_config level=3）',
    `type` VARCHAR(10) NOT NULL DEFAULT 'SINGLE' COMMENT '题型',
    `difficulty` TINYINT NOT NULL DEFAULT 3 COMMENT '难度1-5',
    `content` TEXT NOT NULL COMMENT '题干',
    `options` JSON COMMENT '选项',
    `answer` VARCHAR(10) NOT NULL COMMENT '正确答案',
    `analysis` TEXT COMMENT '预设解析',
    `source` VARCHAR(50) DEFAULT '' COMMENT '来源',
    `frequency` VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '考频',
    `estimated_time` INT DEFAULT 60 COMMENT '预估答题时间(秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_subject_module` (`subject`, `module`),
    INDEX `idx_difficulty` (`difficulty`)
);

CREATE TABLE `practice_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` VARCHAR(10) NOT NULL,
    `is_correct` BOOLEAN NOT NULL,
    `time_spent` INT DEFAULT 0 COMMENT '耗时(秒)',
    `mode` VARCHAR(10) NOT NULL DEFAULT 'PRACTICE' COMMENT 'PRACTICE/EXAM',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_question_id` (`question_id`)
);

CREATE TABLE `error_note` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `error_types` JSON COMMENT '错因选项ID数组（关联error_type_config）',
    `note` VARCHAR(200) DEFAULT '' COMMENT '补充说明',
    `review_count` INT DEFAULT 0,
    `mastered` BOOLEAN DEFAULT FALSE,
    `next_review_at` DATETIME COMMENT '下次复习时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_user_question` (`user_id`, `question_id`)
);

CREATE TABLE `collection` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `note` VARCHAR(500) DEFAULT '',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_user_question` (`user_id`, `question_id`)
);
