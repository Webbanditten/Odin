-- Banned Habbos trying to enter (Odin v1.34.0, OdinCMS Housekeeping > Hobba tracking tools):
-- * banned_login_attempts: a banned player's login (user and the address they came from) or a connection from a banned IP
--   address (no user yet), with the ban that stopped it. BanDao.logBlockedLogin writes it, once per ban, address and user
--   in 10 minutes.
-- Older Odin versions don't write it. Rollback: DROP TABLE banned_login_attempts;

CREATE TABLE IF NOT EXISTS `banned_login_attempts` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `ban_id` INT NOT NULL,
    `ban_type` VARCHAR(20) NOT NULL,
    `user_id` INT NULL,
    `username` VARCHAR(255) NULL,
    `ip` VARCHAR(45) NOT NULL,
    `attempted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `banned_login_attempts_time` (`attempted_at`),
    KEY `banned_login_attempts_recent` (`ban_id`, `ip`, `attempted_at`),
    KEY `banned_login_attempts_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
