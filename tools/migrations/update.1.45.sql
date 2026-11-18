-- In-game ads come from the CMS's cms_ads (Housekeeping > Ads; OdinCMS sql/migrations/2026-11-18_ads.sql, which must
-- be applied first). The old switches and table are unused; Odin creates ads.url, ads.loading.every and ads.pool.every.
DELETE FROM `settings` WHERE `setting` IN ('room.ads', 'room.intersitial.ads');
-- rooms_ads (always empty) is left in place.
