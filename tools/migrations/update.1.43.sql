-- =====================================================================
-- RELEASE MIGRATION - 2026-11-18 (Odin v1.28.0)
-- Trade bans
-- =====================================================================
-- users.trade_banned_until: unix time until which the player may not trade (0 = no trade ban). Set in Housekeeping >
-- Users > Manage. Odin checks it when a trade is opened: the banned player gets the client's "trade_youarenotallowed"
-- alert, the one they asked gets "trade_othernotallowed".
-- =====================================================================

ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `trade_banned_until` bigint(20) NOT NULL DEFAULT 0;
