package org.alexdev.kepler.game.item.interactors.types;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.entities.RoomEntity;
import org.alexdev.kepler.game.room.tasks.PetTask;
import org.alexdev.kepler.game.triggers.GenericTrigger;
import org.apache.commons.lang3.StringUtils;


/**
 * Water bowls: five levels of water (custom data 5 = full, 0 = empty, the client's states). Each drink takes one; a
 * double-click by someone with rights fills it again (the client's "Furniture Waterbowl Class" sends SETSTUFFDATA "5").
 * A new bowl comes full; a bowl keeps its level when it is picked up and put down again.
 */
public class PetWaterBowlInteractor extends GenericTrigger {
    public static final int FULL_LEVEL = 5;
    public static final String FULL = String.valueOf(FULL_LEVEL);

    /** The bowl's water level, 0-5 (anything that isn't a level counts as full). */
    public static int level(Item item) {
        String data = item.getCustomData();

        if (data == null || data.isEmpty() || !StringUtils.isNumeric(data) || data.length() > 2) {
            return FULL_LEVEL;
        }

        return Math.min(FULL_LEVEL, Integer.parseInt(data));
    }

    /** Someone filled a bowl (SETSTUFFDATA): thirsty pets come. */
    public static void filled(Room room) {
        PetFoodInteractor.forEachPet(room, PetTask::foodPlaced);
    }

    @Override
    public void onItemPlaced(Player player, Room room, Item item) {
        String level = String.valueOf(level(item));

        if (!level.equals(item.getCustomData())) {
            item.setCustomData(level);
            item.updateStatus();
            item.save();
        }

        PetFoodInteractor.forEachPet(room, PetTask::foodPlaced);
    }

    @Override
    public void onEntityStop(Entity entity, RoomEntity roomEntity, Item item, boolean isRotation) {
        PetFoodInteractor.reached(entity, item);
    }

    @Override
    public void onItemPickup(Player player, Room room, Item item) {
        PetFoodInteractor.forEachPet(room, task -> task.itemGone(item));
    }

    @Override
    public void onItemMoved(Player player, Room room, Item item, boolean isRotation, Position oldPosition, Item itemBelow, Item itemAbove) {
        if (!isRotation) {
            PetFoodInteractor.forEachPet(room, task -> task.itemGone(item));
        }
    }
}
