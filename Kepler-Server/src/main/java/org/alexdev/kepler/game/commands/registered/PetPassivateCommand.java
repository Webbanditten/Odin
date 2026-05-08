package org.alexdev.kepler.game.commands.registered;

import org.alexdev.kepler.game.commands.Command;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;

public class PetPassivateCommand extends Command {

    @Override
    public void addPermissions() {
        this.permissions.add(Fuse.DEFAULT);
    }

    @Override
    public void addArguments() {
        this.arguments.add("petID");
    }

    @Override
    public void handleCommand(Entity entity, String message, String[] args) {
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) entity;
        Room room = player.getRoomUser().getRoom();

        if (room == null) {
            return;
        }

        int petId;

        try {
            petId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return;
        }

        Entity petEntity = room.getEntityManager().getByInstanceId(petId);

        if (petEntity == null || petEntity.getType() != EntityType.PET) {
            return;
        }

        Pet pet = (Pet) petEntity;

        if (pet.getRoomUser().isWalking()) {
            pet.getRoomUser().stopWalking();
        }

        // Freeze pet AI by setting a very long interaction timer
        if (pet.getRoomUser().getTask() != null) {
            pet.getRoomUser().getTask().setInteractionTimer(Integer.MAX_VALUE);
        }
    }

    @Override
    public String getDescription() {
        return "Freezes a pet's AI (used by pet control panel)";
    }
}
