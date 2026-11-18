-- =====================================================================
-- RELEASE MIGRATION - 2026-11-18 (Odin v1.27.0)
-- Room queues and spectator seats, per room
-- =====================================================================
-- rooms.queue_max       how many may wait in the room's queue when it's full (0 = no queue: "Rummet er fyldt.")
-- rooms.spectators_max  spectator seats (0 = no spectator mode): a spectator watches the room on the client's TV
--                       screen without an avatar (YOUARESPECTATOR) and doesn't count as a visitor
-- Set in Housekeeping > Rooms (the CMS sends update_room, so Odin uses them at once). Habbo Club members wait in
-- front of everyone else.
--
-- Defaults: every public room gets a queue of 50 (as on the original hotel), private rooms none; no room gets
-- spectator seats. Only applied when the columns are new, so re-running this keeps the values set since.
-- =====================================================================

SET @needs_defaults = (SELECT COUNT(*) = 0 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rooms' AND COLUMN_NAME = 'queue_max');

ALTER TABLE `rooms` ADD COLUMN IF NOT EXISTS `queue_max` int(11) NOT NULL DEFAULT 0 AFTER `visitors_max`;
ALTER TABLE `rooms` ADD COLUMN IF NOT EXISTS `spectators_max` int(11) NOT NULL DEFAULT 0 AFTER `queue_max`;

UPDATE `rooms` SET `queue_max` = 50 WHERE @needs_defaults AND `owner_id` = '0';
