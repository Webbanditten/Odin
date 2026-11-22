-- Odin v1.39.0: staff place furni in public rooms (PublicRoomFurni).
-- The fuse shows in Housekeeping > Ranks; it starts with the ranks that already have fuse_any_room_controller.
INSERT IGNORE INTO fuses (fuse, user_group, description)
    VALUES ('fuse_public_room_furni', 'CUSTOM', 'Place, move and pick up furni in public rooms');
INSERT IGNORE INTO rank_rights (rank_id, fuse)
    SELECT rank_id, 'fuse_public_room_furni' FROM rank_rights WHERE fuse = 'fuse_any_room_controller';
-- shown when the furni can't stand in a public room (wall items, teleporters, rollers, pets, presents, ...)
INSERT IGNORE INTO external_texts (entry, text)
    VALUES ('public_room_furni_refused', 'Dette møbel kan ikke stilles i et fællesrum.');

-- Rollback:
-- DELETE FROM rank_rights WHERE fuse = 'fuse_public_room_furni';
-- DELETE FROM fuses WHERE fuse = 'fuse_public_room_furni';
-- DELETE FROM external_texts WHERE entry = 'public_room_furni_refused';
