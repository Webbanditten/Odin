-- Wobble Squabble in the game history (Odin >= v1.25.9): one games_played row per finished duel and a highscores
-- row per player, so the CMS highscore lists ({highscores_wobblesquabble} ...) can rank it like the other games.
--
-- Points (settings, created with these defaults on Odin's first start):
--   wobblesquabble.points.per.hit  10  each hit that lands on the opponent
--   wobblesquabble.points.win      50  knocking the opponent into the water
--   wobblesquabble.increase.points true
-- highscores.team_id is the player's side (0 or 1), team_score their score, won 1 for the winner. A time-out, where
-- both fall in, is no win for either. games_played.winning_team is the winner's side (-1 for a time-out) and
-- extra_data the hits that landed, e.g. "hits 3-5".
--
-- Run before deploying Odin v1.25.9: older enums reject the new game type. Idempotent.

ALTER TABLE `games_played`
  MODIFY `gametype` enum('battleball','snowstorm','wobblesquabble') NOT NULL;

ALTER TABLE `highscores`
  MODIFY `gametype` enum('battleball','snowstorm','wobblesquabble') NOT NULL DEFAULT 'battleball';
