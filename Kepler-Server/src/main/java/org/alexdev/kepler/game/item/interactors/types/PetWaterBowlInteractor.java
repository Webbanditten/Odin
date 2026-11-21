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

import java.util.function.Consumer;

/**
 * Water bowls: "Uendeligt vand", so they never run dry and always show full (state 5).
 */
public class PetWaterBowlInteractor extends GenericTrigger {
    public static final String FULL = "5";

    @Override
    public void onItemPlaced(Player player, Room room, Item item) {
        if (!FULL.equals(item.getCustomData())) {
            item.setCustomData(FULL);
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
