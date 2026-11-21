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
 * Pet food bowls (petfood1-4: four meals, then the bowl is gone) and treats (goodie1/2: one bite). Pets decide when to
 * eat (PetTask); this only tells them when they reach, lose or get food.
 */
public class PetFoodInteractor extends GenericTrigger {
    @Override
    public void onItemPlaced(Player player, Room room, Item item) {
        if (item.getCustomData().isBlank()) {
            item.setCustomData("0");
            item.updateStatus();
            item.save();
        }

        forEachPet(room, PetTask::foodPlaced);
    }

    @Override
    public void onEntityStop(Entity entity, RoomEntity roomEntity, Item item, boolean isRotation) {
        reached(entity, item);
    }

    @Override
    public void onItemPickup(Player player, Room room, Item item) {
        forEachPet(room, task -> task.itemGone(item));
    }

    @Override
    public void onItemMoved(Player player, Room room, Item item, boolean isRotation, Position oldPosition, Item itemBelow, Item itemAbove) {
        if (!isRotation) {
            forEachPet(room, task -> task.itemGone(item));
        }
    }

    /** A pet stopped on a pet item. */
    static void reached(Entity entity, Item item) {
        if (entity.getType() != EntityType.PET) {
            return;
        }

        PetTask task = ((Pet) entity).getRoomUser().getTask();

        if (task != null) {
            task.reachedItem(item);
        }
    }

    static void forEachPet(Room room, Consumer<PetTask> action) {
        for (Pet pet : room.getEntityManager().getEntitiesByClass(Pet.class)) {
            PetTask task = pet.getRoomUser().getTask();

            if (task != null) {
                action.accept(task);
            }
        }
    }
}
