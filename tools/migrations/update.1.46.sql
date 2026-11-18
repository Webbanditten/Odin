-- Public room navigator order (Housekeeping > Public rooms, Odin v1.31.0): the navigator lists the public rooms of a
-- category by navigator_order, then id (busiest first while navigator.public.busiest.first is true, the default, so the
-- order decides between rooms with as many visitors). Seeded with the id order the navigator already showed for empty rooms.
-- The public categories under Fællesrum (3) get an order_id in id order: Odin never read order_id before, so players saw them
-- in id order (Habbo Club, SnowStorm, Battle Ball), and this keeps it.
-- Older Odin and CMS versions ignore the column. Rollback: ALTER TABLE rooms DROP COLUMN navigator_order;

ALTER TABLE `rooms` ADD COLUMN `navigator_order` INT NOT NULL DEFAULT 0 AFTER `category`;
UPDATE `rooms` SET `navigator_order` = `id` WHERE `owner_id` = '0';

SET @place := 0;
UPDATE `rooms_categories` SET `order_id` = (@place := @place + 1) WHERE `parent_id` = 3 AND `public_spaces` = 1 ORDER BY `id`;
