package org.alexdev.kepler.game.item.interactors.types;

import org.alexdev.kepler.dao.mysql.PetDao;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.pets.PetDetails;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.entities.RoomEntity;
import org.alexdev.kepler.game.triggers.GenericTrigger;

/**
 * A pet's basket ("Kurv"): placing it brings the pet into the room, picking it up takes the pet away, and the pet
 * sleeps in it (PetTask).
 */
public class PetNestInteractor extends GenericTrigger {
    @Override
    public void onItemPlaced(Player player, Room room, Item item) {
        PetDetails petDetails = PetDao.getPetDetails(item.getId());

        if (petDetails != null) {
            Pet pet = this.addPet(room, petDetails, item.getPosition().copy(), item);
            pet.saveDetails();
        }
    }

    @Override
    public void onItemPickup(Player player, Room room, Item item) {
        PetDetails petDetails = PetDao.getPetDetails(item.getId());

        if (petDetails == null) {
            return;
        }

        Pet pet = (Pet) room.getEntityManager().getById(petDetails.getId(), EntityType.PET);

        if (pet == null) {
            return;
        }

        pet.saveDetails();
        room.getEntityManager().leaveRoom(pet, false);
    }

    @Override
    public void onEntityStop(Entity entity, RoomEntity roomEntity, Item item, boolean isRotation) {
        if (entity.getType() == EntityType.PET && ((Pet) entity).getDetails().getItemId() == item.getId()) {
            PetFoodInteractor.reached(entity, item);
        }
    }

    /**
     * Add a pet to a room.
     *
     * @param room the room to add the pet to
     * @param petDetails the details of the pet
     * @param position the position of the pet
     * @param nest the pet's basket (its owner owns the pet)
     *
     * @return the pet instance created
     */
    public Pet addPet(Room room, PetDetails petDetails, Position position, Item nest) {
        Pet pet = new Pet(petDetails);
        pet.setOwnerId(nest.getOwnerId());
        position.setZ(room.getMapping().getTile(position.getX(), position.getY()).getWalkingHeight());

        room.getEntityManager().enterRoom(pet, position);
        room.getMapping().getTile(position).addEntity(pet);

        pet.getRoomUser().createTask(room);
        pet.getRoomUser().getTask().onLoaded();

        return pet;
    }
}
