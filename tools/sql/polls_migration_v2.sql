-- =====================================================
-- Polls System Migration v2
-- Fixes schema issues and adds missing columns
-- Run this AFTER the original polls.sql has been applied
-- =====================================================

-- --------------------------------------------------------
-- 1. Fix polls_questions: Add missing columns (text, min_select, max_select, order)
-- --------------------------------------------------------

ALTER TABLE `polls_questions`
  ADD COLUMN IF NOT EXISTS `text` text NOT NULL AFTER `type`,
  ADD COLUMN IF NOT EXISTS `min_select` int(11) NOT NULL DEFAULT 0 AFTER `text`,
  ADD COLUMN IF NOT EXISTS `max_select` int(11) NOT NULL DEFAULT 1 AFTER `min_select`,
  ADD COLUMN IF NOT EXISTS `order` int(11) NOT NULL DEFAULT 0 AFTER `max_select`;

-- --------------------------------------------------------
-- 2. Fix polls_questions type column: Use VARCHAR(20) instead of enum
--    for compatibility with EF Core and to allow future question types
-- --------------------------------------------------------

ALTER TABLE `polls_questions`
  MODIFY COLUMN `type` varchar(20) NOT NULL DEFAULT 'CHOICE';

-- --------------------------------------------------------
-- 3. Fix polls_answers: Add user_id, poll_id columns and fix FK
-- --------------------------------------------------------

-- Drop the incorrect FK constraint (points at polls instead of polls_questions)
ALTER TABLE `polls_answers`
  DROP FOREIGN KEY IF EXISTS `polls_questions_polls_answers_FK`;

-- Add missing columns
ALTER TABLE `polls_answers`
  ADD COLUMN IF NOT EXISTS `user_id` int(11) NOT NULL AFTER `poll_question_id`,
  ADD COLUMN IF NOT EXISTS `poll_id` int(11) NOT NULL AFTER `user_id`;

-- Add correct FK to polls_questions
ALTER TABLE `polls_answers`
  ADD CONSTRAINT `polls_answers_question_FK` FOREIGN KEY (`poll_question_id`) REFERENCES `polls_questions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

-- Add FK to polls
ALTER TABLE `polls_answers`
  ADD CONSTRAINT `polls_answers_poll_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

-- Add index on user_id for lookup performance
ALTER TABLE `polls_answers`
  ADD INDEX IF NOT EXISTS `idx_polls_answers_user` (`user_id`);

-- Note: No unique constraint on (poll_question_id, user_id) because MULTI_CHOICE
-- questions require multiple answer rows per user per question (one per selected option).
-- Duplicate prevention is handled at the application level via hasUserAnswered().
ALTER TABLE `polls_answers`
  DROP INDEX IF EXISTS `idx_polls_answers_unique`;

-- --------------------------------------------------------
-- 4. Add order column to polls_questions_options
-- --------------------------------------------------------

ALTER TABLE `polls_questions_options`
  ADD COLUMN IF NOT EXISTS `order` int(11) NOT NULL DEFAULT 0 AFTER `poll_question_id`;

-- --------------------------------------------------------
-- 5. Add enabled flag and timestamps to polls
-- --------------------------------------------------------

ALTER TABLE `polls`
  ADD COLUMN IF NOT EXISTS `enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `description`,
  ADD COLUMN IF NOT EXISTS `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `enabled`;

-- --------------------------------------------------------
-- 6. Add status and timestamp to polls_offers
-- --------------------------------------------------------

ALTER TABLE `polls_offers`
  ADD COLUMN IF NOT EXISTS `status` enum('ACCEPTED','REJECTED') NOT NULL DEFAULT 'ACCEPTED' AFTER `poll_id`,
  ADD COLUMN IF NOT EXISTS `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `status`;

-- Add unique constraint to prevent duplicate offer records
ALTER TABLE `polls_offers`
  ADD UNIQUE INDEX IF NOT EXISTS `idx_polls_offers_unique` (`poll_id`, `user_id`);
