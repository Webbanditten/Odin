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
 * The ball (toy1): pets play with it when they are bored or unhappy (PetTask), and it turns while they do.
 */
public class PetToyInteractor extends GenericTrigger {
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
