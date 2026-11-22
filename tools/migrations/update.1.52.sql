-- Furni made in the Housekeeping furni editor (OdinCMS docs/furni-editor-housekeeping.md): one row per publish.
-- The CMS writes it; Odin sends the highest revision per sprite in the furni revision list (SPRITE_LIST, 295), so
-- clients whose dynamic.download.name.template has %revision% download a new revision instead of a cached cast.
-- A sprite with rows here was made in the editor: only those casts may be published over.
CREATE TABLE IF NOT EXISTS `furni_versions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sprite` varchar(64) NOT NULL,
  `revision` int(11) NOT NULL,
  `large_sha256` char(64) NOT NULL,
  `large_bytes` int(11) NOT NULL,
  `small_sha256` char(64) DEFAULT NULL,
  `small_bytes` int(11) DEFAULT NULL,
  `definition_id` int(11) DEFAULT NULL,
  `definition` text DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `source` varchar(16) NOT NULL DEFAULT 'editor',
  `rolled_back_from` int(11) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sprite_revision` (`sprite`, `revision`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rollback: DROP TABLE furni_versions; (after removing the casts it lists from hof_furni, if they should go too)
