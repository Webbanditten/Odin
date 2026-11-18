-- =====================================================================
-- RELEASE MIGRATION - 2026-11-18 (Odin v1.29.0)
-- Public room shows (docs/room-programs.md)
-- =====================================================================
-- These rooms' client scripts change their disco floors / lights / lamps only when the server sends SHOWPROGRAM,
-- which Odin never did: Club Mammoth (club_mammoth), Café Solnedgang (sunset_cafe), Club Orient (orient), Club 57's
-- dance floor (malja_bar_b) and Det orange Rum (bar_b). RoomProgramTrigger drives them (setting room.programs.seconds).
-- Only rooms that have no trigger yet are changed.
-- =====================================================================

ALTER TABLE `rooms_models` CHANGE `trigger_class` `trigger_class` ENUM('flat_trigger','battleball_lobby_trigger','snowstorm_lobby_trigger','space_cafe_trigger','habbo_lido_trigger','rooftop_rumble_trigger','diving_deck_trigger','infobus_park','infobus_poll','room_program_trigger','none') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'flat_trigger';

UPDATE `rooms_models` SET `trigger_class` = 'room_program_trigger'
WHERE `model_id` IN ('club_mammoth', 'sunset_cafe', 'orient', 'malja_bar_b', 'bar_b')
  AND (`trigger_class` IS NULL OR `trigger_class` = '' OR `trigger_class` = 'none');
