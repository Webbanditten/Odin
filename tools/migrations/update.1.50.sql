-- =====================================================================
-- Odin v1.35.0 - pets (docs/pets.md)
-- =====================================================================
-- Water bowls never run dry now ("Uendeligt vand"): fill the ones the old code emptied (custom data 5 = full).
-- Friendship (0-100, with the owner) is used now; every pet still has the column default 1, so start them at 30 like a
-- new pet.
--
-- Safe with the older Odin running (it reads both and ignores friendship). Rollback: not needed (the old code
-- empties bowls again as pets drink, and doesn't read friendship).
-- =====================================================================

UPDATE items i
JOIN items_definitions d ON d.id = i.definition_id
SET i.custom_data = '5'
WHERE d.interactor = 'pet_water_bowl' AND i.custom_data <> '5';

UPDATE items_pets SET friendship = 30 WHERE friendship <= 1;
