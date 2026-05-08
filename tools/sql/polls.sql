-- =====================================================
-- Polls System - Complete Schema
-- Safe to run on fresh DB or existing DB (fully idempotent)
-- =====================================================

-- --------------------------------------------------------
-- Tables
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `polls` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `headline` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thank_you` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `polls_questions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poll_id` int(11) NOT NULL,
  `type` varchar(20) NOT NULL DEFAULT 'CHOICE',
  `text` text NOT NULL,
  `min_select` int(11) NOT NULL DEFAULT 0,
  `max_select` int(11) NOT NULL DEFAULT 1,
  `order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `polls_polls_questions_FK` (`poll_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `polls_questions_options` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `poll_question_id` int(11) NOT NULL,
  `order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `polls_questions_polls_questions_options_FK` (`poll_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `polls_answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poll_question_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `poll_id` int(11) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_polls_answers_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `polls_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `poll_id` int(11) NOT NULL,
  `status` enum('ACCEPTED','REJECTED') NOT NULL DEFAULT 'ACCEPTED',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_polls_offers_unique` (`poll_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `polls_triggers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poll_id` int(11) NOT NULL,
  `room` int(11) DEFAULT NULL,
  `time_from` int(11) DEFAULT NULL,
  `time_to` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `polls_polls_triggers_FK` (`poll_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------
-- Migrate existing tables (safe if columns/indexes already exist)
-- --------------------------------------------------------

ALTER TABLE `polls`
  ADD COLUMN IF NOT EXISTS `enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `description`,
  ADD COLUMN IF NOT EXISTS `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `enabled`;

ALTER TABLE `polls_questions`
  ADD COLUMN IF NOT EXISTS `text` text NOT NULL AFTER `type`,
  ADD COLUMN IF NOT EXISTS `min_select` int(11) NOT NULL DEFAULT 0 AFTER `text`,
  ADD COLUMN IF NOT EXISTS `max_select` int(11) NOT NULL DEFAULT 1 AFTER `min_select`,
  ADD COLUMN IF NOT EXISTS `order` int(11) NOT NULL DEFAULT 0 AFTER `max_select`;

ALTER TABLE `polls_questions`
  MODIFY COLUMN `type` varchar(20) NOT NULL DEFAULT 'CHOICE';

ALTER TABLE `polls_questions_options`
  ADD COLUMN IF NOT EXISTS `order` int(11) NOT NULL DEFAULT 0 AFTER `poll_question_id`;

ALTER TABLE `polls_answers`
  ADD COLUMN IF NOT EXISTS `user_id` int(11) NOT NULL AFTER `poll_question_id`,
  ADD COLUMN IF NOT EXISTS `poll_id` int(11) NOT NULL AFTER `user_id`;

ALTER TABLE `polls_answers`
  ADD INDEX IF NOT EXISTS `idx_polls_answers_user` (`user_id`);

-- Remove overly-restrictive unique constraint (breaks MULTI_CHOICE which needs multiple rows per user)
ALTER TABLE `polls_answers`
  DROP INDEX IF EXISTS `idx_polls_answers_unique`;

ALTER TABLE `polls_offers`
  ADD COLUMN IF NOT EXISTS `status` enum('ACCEPTED','REJECTED') NOT NULL DEFAULT 'ACCEPTED' AFTER `poll_id`,
  ADD COLUMN IF NOT EXISTS `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `status`;

ALTER TABLE `polls_offers`
  ADD UNIQUE INDEX IF NOT EXISTS `idx_polls_offers_unique` (`poll_id`, `user_id`);

-- --------------------------------------------------------
-- Foreign keys (drop first if exist, then re-add)
-- --------------------------------------------------------

ALTER TABLE `polls_answers`
  DROP FOREIGN KEY IF EXISTS `polls_questions_polls_answers_FK`,
  DROP FOREIGN KEY IF EXISTS `polls_answers_question_FK`,
  DROP FOREIGN KEY IF EXISTS `polls_answers_poll_FK`;

ALTER TABLE `polls_answers`
  ADD CONSTRAINT `polls_answers_question_FK` FOREIGN KEY (`poll_question_id`) REFERENCES `polls_questions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  ADD CONSTRAINT `polls_answers_poll_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

ALTER TABLE `polls_offers`
  DROP FOREIGN KEY IF EXISTS `polls_polls_offers_FK`;

ALTER TABLE `polls_offers`
  ADD CONSTRAINT `polls_polls_offers_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

ALTER TABLE `polls_questions`
  DROP FOREIGN KEY IF EXISTS `polls_polls_questions_FK`;

ALTER TABLE `polls_questions`
  ADD CONSTRAINT `polls_polls_questions_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

ALTER TABLE `polls_questions_options`
  DROP FOREIGN KEY IF EXISTS `polls_questions_polls_questions_options_FK`;

ALTER TABLE `polls_questions_options`
  ADD CONSTRAINT `polls_questions_polls_questions_options_FK` FOREIGN KEY (`poll_question_id`) REFERENCES `polls_questions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

ALTER TABLE `polls_triggers`
  DROP FOREIGN KEY IF EXISTS `polls_polls_triggers_FK`;

ALTER TABLE `polls_triggers`
  ADD CONSTRAINT `polls_polls_triggers_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;
