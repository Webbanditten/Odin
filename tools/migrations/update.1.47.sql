-- Rolling room ratings (Odin v1.32.0, game/room/RoomRatings.java). Votes are kept from now on (the old code deleted a vote
-- 30 days after it was cast, whenever the navigator listed the room empty):
-- * voted_at is when a vote was cast, expire_time when its voter may vote on the room again (voted_at + room.rating.vote.days,
--   7). Existing votes: voted_at = their old expire_time - 30 days, expire_time = voted_at + 7 days.
-- * rooms.rating ("Rummets karakter") = the sum of all the room's votes, never below 0.
-- * The day's / week's best rooms count the votes cast in the last 24 hours / 7 days; "Anbefalede Rum" are the week's best
--   open rooms, and three new guest room categories (rooms_categories.ranking) list the day's, week's and all-time best.
-- Older Odin versions ignore voted_at and ranking (they'd list the ranking categories as empty categories, and the 30-day rule
-- comes back). Rollback: DELETE FROM rooms_categories WHERE ranking <> ''; then drop the two columns.

ALTER TABLE `users_room_votes`
    ADD COLUMN `voted_at` BIGINT NOT NULL DEFAULT 0 AFTER `vote`,
    ADD INDEX `room_votes_room` (`room_id`, `voted_at`),
    ADD INDEX `room_votes_time` (`voted_at`),
    ADD INDEX `room_votes_voter` (`room_id`, `user_id`, `expire_time`);
UPDATE `users_room_votes` SET `voted_at` = `expire_time` - 2592000 WHERE `voted_at` = 0;
UPDATE `users_room_votes` SET `expire_time` = `voted_at` + 604800;

UPDATE `rooms` r LEFT JOIN (SELECT `room_id`, SUM(`vote`) AS `total` FROM `users_room_votes` GROUP BY `room_id`) v ON v.`room_id` = r.`id`
SET r.`rating` = GREATEST(0, COALESCE(v.`total`, 0)) WHERE r.`owner_id` <> '0';

ALTER TABLE `rooms_categories` ADD COLUMN `ranking` VARCHAR(10) NOT NULL DEFAULT '' AFTER `fuse_setflatcat`;
-- Under "Gæsterum" (4), shown above the other guest room categories. Nobody has fuse_ranking_category, so players can't put a
-- room in them (Odin refuses it anyway).
INSERT INTO `rooms_categories` (`order_id`, `parent_id`, `isnode`, `name`, `public_spaces`, `allow_trading`, `minrole_access`, `minrole_setflatcat`, `fuse_access`, `fuse_setflatcat`, `ranking`) VALUES
    (1, 4, 0, 'Dagens bedst bedømte rum', 0, 0, 1, 6, 'DEFAULT', 'fuse_ranking_category', 'daily'),
    (2, 4, 0, 'Ugens bedst bedømte rum', 0, 0, 1, 6, 'DEFAULT', 'fuse_ranking_category', 'weekly'),
    (3, 4, 0, 'Bedst bedømte rum nogensinde', 0, 0, 1, 6, 'DEFAULT', 'fuse_ranking_category', 'alltime');
