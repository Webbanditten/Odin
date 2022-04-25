CREATE TABLE IF NOT EXISTS `polls`
(
 `id`          int NOT NULL AUTO_INCREMENT ,
 `headline`    varchar(255) NOT NULL ,
 `thank_you`   varchar(255) NOT NULL ,
 `description` text NOT NULL ,

PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `polls_answers`
(
 `id`               int NOT NULL AUTO_INCREMENT ,
 `poll_question_id` int NOT NULL ,
 `value`            text NOT NULL ,

PRIMARY KEY (`id`),
KEY `FK_34` (`poll_question_id`),
CONSTRAINT `polls_questions_polls_answers_fk` FOREIGN KEY `FK_34` (`poll_question_id`) REFERENCES `polls_questions` (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `polls_offers`
(
 `id`      int NOT NULL AUTO_INCREMENT ,
 `user_id` int NOT NULL ,
 `poll_id` int NOT NULL ,

PRIMARY KEY (`id`),
KEY `FK_66` (`poll_id`),
CONSTRAINT `polls_polls_offers_fk` FOREIGN KEY `FK_66` (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `polls_questions`
(
 `id`      int NOT NULL AUTO_INCREMENT ,
 `poll_id` int NOT NULL ,
 `type`    enum('choice', 'text') NOT NULL ,

PRIMARY KEY (`id`),
KEY `FK_23` (`poll_id`),
CONSTRAINT `polls_polls_questions_fk` FOREIGN KEY `FK_23` (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `polls_questions_options`
(
 `id`               int NOT NULL AUTO_INCREMENT ,
 `name`             varchar(255) NOT NULL ,
 `poll_question_id` int NOT NULL ,

PRIMARY KEY (`id`),
KEY `FK_50` (`poll_question_id`),
CONSTRAINT `polls_questions_polls_questions_options_fk` FOREIGN KEY `FK_50` (`poll_question_id`) REFERENCES `polls_questions` (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `polls_triggers`
(
 `id`        int NOT NULL AUTO_INCREMENT ,
 `poll_id`   int NOT NULL ,
 `room`      int NULL ,
 `time_from` int NULL ,
 `time_to`   int NULL ,

PRIMARY KEY (`id`),
KEY `FK_39` (`poll_id`),
CONSTRAINT `polls_polls_triggers_fk` FOREIGN KEY `FK_39` (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE
);

