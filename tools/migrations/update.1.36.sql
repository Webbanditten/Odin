-- Game history for highscores and challenges.
--
-- games_played: one row per finished Battle Ball / SnowStorm game that counts for points
-- (battleball.increase.points / snowstorm.increase.points).
-- highscores: each player's row now also names the game, their team, the team's score and whether
-- that team won (a tie for first is not a win). Rows from before this migration keep NULLs there.
--
-- Weekly top list, for example:
--   SELECT u.username, SUM(h.score) AS score FROM highscores h JOIN users u ON u.id = h.uid
--   WHERE h.gametype = 'battleball' AND h.date >= CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY
--   GROUP BY h.uid ORDER BY score DESC LIMIT 10;

CREATE TABLE IF NOT EXISTS `games_played` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `gametype` enum('battleball','snowstorm') NOT NULL,
  `name` varchar(100) NOT NULL DEFAULT '',
  `creator` varchar(50) NOT NULL DEFAULT '',
  `map_id` int(11) NOT NULL DEFAULT 0,
  `team_count` int(11) NOT NULL DEFAULT 0,
  `player_count` int(11) NOT NULL DEFAULT 0,
  `winning_team` int(11) NOT NULL DEFAULT -1,
  `winning_team_score` int(11) NOT NULL DEFAULT 0,
  `extra_data` varchar(255) NOT NULL DEFAULT '',
  `finished_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `gametype_finished_at` (`gametype`, `finished_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `highscores`
  ADD COLUMN IF NOT EXISTS `game_id` int(11) NULL DEFAULT NULL AFTER `score`,
  ADD COLUMN IF NOT EXISTS `team_id` int(11) NULL DEFAULT NULL AFTER `game_id`,
  ADD COLUMN IF NOT EXISTS `team_score` int(11) NULL DEFAULT NULL AFTER `team_id`,
  ADD COLUMN IF NOT EXISTS `won` tinyint(1) NULL DEFAULT NULL AFTER `team_score`,
  ADD KEY IF NOT EXISTS `gametype_date` (`gametype`, `date`),
  ADD KEY IF NOT EXISTS `game_id` (`game_id`);
