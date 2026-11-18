-- Navigator category management (Odin v1.33.0, OdinCMS Housekeeping > Room categories and Public rooms):
-- * rooms_categories.is_hidden: the navigator doesn't list or open the category and players can't put a room in it.
-- * The guest room categories under Gæsterum (4) get an order_id in id order (Odin never read it for them, so players saw
--   id order between categories with as many visitors), so the order stays as it is. The best rated lists keep 1-3.
-- * Setting navigator.private.busiest.first (created by Odin as true): busiest first, then the order; false = the order.
-- Older Odin versions ignore is_hidden (they'd list hidden categories). Rollback: drop the column.

ALTER TABLE `rooms_categories` ADD COLUMN `is_hidden` TINYINT(1) NOT NULL DEFAULT 0 AFTER `ranking`;

SET @place := 0;
UPDATE `rooms_categories` SET `order_id` = (@place := @place + 1)
WHERE `parent_id` = 4 AND `public_spaces` = 0 AND `ranking` = '' ORDER BY `id`;
