
--
-- Table structure for table `polls`
--

CREATE TABLE IF NOT EXISTS `polls` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `headline` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thank_you` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `polls_questions`
--

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

-- --------------------------------------------------------

--
-- Table structure for table `polls_questions_options`
--

CREATE TABLE IF NOT EXISTS `polls_questions_options` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `poll_question_id` int(11) NOT NULL,
  `order` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `polls_questions_polls_questions_options_FK` (`poll_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `polls_answers`
--

CREATE TABLE IF NOT EXISTS `polls_answers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poll_question_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `poll_id` int(11) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `polls_answers_question_FK` (`poll_question_id`),
  KEY `polls_answers_poll_FK` (`poll_id`),
  KEY `idx_polls_answers_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `polls_offers`
--

CREATE TABLE IF NOT EXISTS `polls_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `poll_id` int(11) NOT NULL,
  `status` enum('ACCEPTED','REJECTED') NOT NULL DEFAULT 'ACCEPTED',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `polls_polls_offers_FK` (`poll_id`),
  UNIQUE KEY `idx_polls_offers_unique` (`poll_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `polls_triggers`
--

CREATE TABLE IF NOT EXISTS `polls_triggers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `poll_id` int(11) NOT NULL,
  `room` int(11) DEFAULT NULL,
  `time_from` int(11) DEFAULT NULL,
  `time_to` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `polls_polls_triggers_FK` (`poll_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `polls_answers`
--
ALTER TABLE `polls_answers`
  ADD CONSTRAINT `polls_answers_question_FK` FOREIGN KEY (`poll_question_id`) REFERENCES `polls_questions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

ALTER TABLE `polls_answers`
  ADD CONSTRAINT `polls_answers_poll_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `polls_offers`
--
ALTER TABLE `polls_offers`
  ADD CONSTRAINT `polls_polls_offers_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `polls_questions`
--
ALTER TABLE `polls_questions`
  ADD CONSTRAINT `polls_polls_questions_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `polls_questions_options`
--
ALTER TABLE `polls_questions_options`
  ADD CONSTRAINT `polls_questions_polls_questions_options_FK` FOREIGN KEY (`poll_question_id`) REFERENCES `polls_questions` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;

--
-- Constraints for table `polls_triggers`
--
ALTER TABLE `polls_triggers`
  ADD CONSTRAINT `polls_polls_triggers_FK` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;
COMMIT;
