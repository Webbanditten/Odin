-- Purchase log: one row per purchase, in the game (Odin) and on the website (CMS).
--
-- kind: catalogue (furni bought for yourself), gift (furni bought for receiver_id), club (Habbo Club),
--       tickets (game tickets, receiver_id when bought for someone else), song (burned trax disc),
--       web_furni, home_store (Habbo Home stickers/backgrounds/widgets), group (website).
-- page_fuse: the catalogue page's fuse at purchase time ('DEFAULT' and 'fuse_catalogue_clubshop' are the
--            player pages; others such as 'fuse_catalogue_administrator' are staff pages).
-- credit_log keeps feeding the in-game purse; this table says what was bought.

CREATE TABLE IF NOT EXISTS `purchase_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `kind` varchar(20) NOT NULL,
  `receiver_id` int(11) NULL DEFAULT NULL,
  `catalogue_item_id` int(11) NULL DEFAULT NULL,
  `definition_id` int(11) NULL DEFAULT NULL,
  `item_name` varchar(255) NOT NULL DEFAULT '',
  `credits` int(11) NOT NULL DEFAULT 0,
  `page_fuse` varchar(100) NULL DEFAULT NULL,
  `source` varchar(10) NOT NULL DEFAULT 'game',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `user_created` (`user_id`, `created_at`),
  KEY `kind_created` (`kind`, `created_at`),
  KEY `definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
