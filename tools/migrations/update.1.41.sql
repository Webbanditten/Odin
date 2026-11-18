-- =====================================================================
-- RELEASE MIGRATION - 2026-11-18 (Odin v1.26.0)
-- Habbo Club membership history: real membership time for months, the gold badge and gifts
-- =====================================================================
-- Before: "months as member" and the gold HC badge counted calendar time since the very first subscription (gaps
-- included), and gifts ran on a calendar timer (club_gift_due). Now:
--   users_club_periods         one row per unbroken membership period; a purchase while subscribed extends the open one
--   users.club_gifts_claimed   gifts received so far = the position in the gift order (welcome gift = 0)
--   users.club_gift_member_seconds  membership time already paid out as gifts
-- Months as member = time inside the periods / 31 days, the gold badge from 12. A gift is due for every gift interval
-- (settings club.gift.interval + club.gift.timeunit) of membership past club_gift_member_seconds.
--
-- Backfill: the periods are rebuilt by replaying every Habbo Club purchase in credit_log (type club_habbo: 25 credits
-- = 31 days, 60 = 93, 105 = 186) the way ClubSubscription extends a subscription; the open period then ends at
-- users.club_expiration. The welcome gift (club_sofa, never logged before) is added to users_club_gifts at the first
-- subscription. club_gifts_claimed = the gifts on record, so everyone continues the gift order where they are, and
-- club_gift_member_seconds = the gift intervals already reached, so nobody gets a backlog ("start fresh").
--
-- Idempotent: the backfill only runs while users_club_periods is empty.
-- =====================================================================

ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `club_gifts_claimed` int(11) NOT NULL DEFAULT 0 AFTER `club_gift_due`;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `club_gift_member_seconds` bigint(20) NOT NULL DEFAULT 0 AFTER `club_gifts_claimed`;

CREATE TABLE IF NOT EXISTS `users_club_periods` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `started_at` bigint(20) NOT NULL,
  `ends_at` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`, `ends_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @needs_backfill = (SELECT COUNT(*) = 0 FROM users_club_periods);
SET @gift_seconds = COALESCE((SELECT CAST(value AS UNSIGNED) FROM settings WHERE setting = 'club.gift.interval'), 31)
    * (CASE COALESCE((SELECT value FROM settings WHERE setting = 'club.gift.timeunit'), 'DAYS')
         WHEN 'SECONDS' THEN 1 WHEN 'MINUTES' THEN 60 WHEN 'HOURS' THEN 3600 ELSE 86400 END);
SET @now = UNIX_TIMESTAMP();

-- Purchases in order per player (a helper table: a TEMPORARY one can't be read twice in one query).
DROP TABLE IF EXISTS `tmp_club_purchases`;
CREATE TABLE `tmp_club_purchases` AS
SELECT user_id, `timestamp` AS t,
       CASE credits WHEN -25 THEN 31 WHEN -60 THEN 93 WHEN -105 THEN 186 ELSE 0 END * 86400 AS secs,
       ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY `timestamp`, id) AS rn
FROM credit_log
WHERE type = 'club_habbo' AND @needs_backfill AND user_id IN (SELECT id FROM users);
ALTER TABLE `tmp_club_purchases` ADD INDEX (user_id, rn);

-- Replay: a purchase after the subscription ran out starts a new period (+1 s, as ClubSubscription.subscribeClub),
-- otherwise it extends the running one.
DROP TABLE IF EXISTS `tmp_club_chain`;
CREATE TABLE `tmp_club_chain` AS
WITH RECURSIVE chain AS (
    SELECT user_id, rn, t AS starts, t + secs + 1 AS ends, 1 AS new_period
    FROM tmp_club_purchases WHERE rn = 1
    UNION ALL
    SELECT p.user_id, p.rn,
           IF(p.t >= c.ends, p.t, c.starts),
           IF(p.t >= c.ends, p.t + p.secs + 1, c.ends + p.secs),
           IF(p.t >= c.ends, 1, 0)
    FROM chain c JOIN tmp_club_purchases p ON p.user_id = c.user_id AND p.rn = c.rn + 1
)
SELECT user_id, rn, starts, ends, SUM(new_period) OVER (PARTITION BY user_id ORDER BY rn) AS period
FROM chain;

INSERT INTO users_club_periods (user_id, started_at, ends_at)
SELECT user_id, MIN(starts), MAX(ends) FROM tmp_club_chain GROUP BY user_id, period ORDER BY user_id, period;

DROP TABLE IF EXISTS `tmp_club_chain`;
DROP TABLE IF EXISTS `tmp_club_purchases`;

-- The open period of a current member ends at their expiration (the authority; matches the replay to the second).
UPDATE users_club_periods p
JOIN users u ON u.id = p.user_id
JOIN (SELECT user_id, MAX(ends_at) AS last_end FROM users_club_periods GROUP BY user_id) l ON l.user_id = p.user_id AND l.last_end = p.ends_at
SET p.ends_at = u.club_expiration
WHERE @needs_backfill AND u.club_expiration > @now AND u.club_expiration > p.started_at;

-- Members with no purchase on record (granted by hand): one period from their first subscription to their expiration.
INSERT INTO users_club_periods (user_id, started_at, ends_at)
SELECT u.id, u.club_subscribed, u.club_expiration FROM users u
WHERE @needs_backfill AND u.club_subscribed > 0 AND u.club_expiration > u.club_subscribed
  AND NOT EXISTS (SELECT 1 FROM users_club_periods p WHERE p.user_id = u.id);

-- The welcome gift was handed out at the first subscription but never logged.
INSERT INTO users_club_gifts (user_id, sprite, date_received)
SELECT u.id, 'club_sofa', u.club_subscribed FROM users u
WHERE @needs_backfill AND u.club_subscribed > 0
  AND NOT EXISTS (SELECT 1 FROM users_club_gifts g WHERE g.user_id = u.id AND g.sprite = 'club_sofa' AND g.date_received <= u.club_subscribed + 60);

-- Continue the gift order where each player is.
UPDATE users u
JOIN (SELECT user_id, COUNT(*) AS gifts FROM users_club_gifts GROUP BY user_id) g ON g.user_id = u.id
SET u.club_gifts_claimed = g.gifts
WHERE @needs_backfill;

-- Start fresh: the gift intervals reached so far count as paid out; the next gift comes at the next one.
UPDATE users u
JOIN (SELECT user_id, SUM(GREATEST(0, LEAST(ends_at, @now) - started_at)) AS member_secs FROM users_club_periods GROUP BY user_id) m ON m.user_id = u.id
SET u.club_gift_member_seconds = FLOOR(m.member_secs / @gift_seconds) * @gift_seconds
WHERE @needs_backfill;
