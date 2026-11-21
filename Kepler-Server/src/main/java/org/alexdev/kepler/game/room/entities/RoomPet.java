package org.alexdev.kepler.game.room.entities;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.enums.StatusType;
import org.alexdev.kepler.game.room.tasks.PetTask;

public class RoomPet extends RoomEntity {
    private final Pet pet;
    private PetTask task;

    public RoomPet(Entity entity) {
        super(entity);
        this.pet = (Pet) entity;
    }

    /**
     * A step: no sitting, lying, sniffing etc. while walking (faces and the wagging tail stay).
     */
    public void clearPoses() {
        for (StatusType type : PetTask.POSES) {
            this.removeStatus(type);
        }
    }

    public PetTask getTask() {
        return task;
    }

    public void createTask(Room room) {
        this.task = new PetTask(this.pet, room);
    }
}
