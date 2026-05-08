package org.alexdev.kepler.game.commands.registered;

import org.alexdev.kepler.game.commands.Command;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;

public class PetSetDirCommand extends Command {

    // Direction offsets indexed by rotation (0-7): N, NE, E, SE, S, SW, W, NW
    private static final int[] DIRECTION_X = {  0,  1,  1,  1,  0, -1, -1, -1 };
    private static final int[] DIRECTION_Y = { -1, -1,  0,  1,  1,  1,  0, -1 };

    @Override
    public void addPermissions() {
        this.permissions.add(Fuse.DEFAULT);
    }

    @Override
    public void addArguments() {
        this.arguments.add("petID");
        this.arguments.add("headRot");
        this.arguments.add("bodyRot");
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
        int headRot;
        int bodyRot;

        try {
            petId = Integer.parseInt(args[0]);
            headRot = Integer.parseInt(args[1]);
            bodyRot = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return;
        }

        // Validate rotation values (0-7)
        if (headRot < 0 || headRot > 7 || bodyRot < 0 || bodyRot > 7) {
            return;
        }

        Entity petEntity = room.getEntityManager().getByInstanceId(petId);

        if (petEntity == null || petEntity.getType() != EntityType.PET) {
            return;
        }

        Pet pet = (Pet) petEntity;
        Position pos = pet.getRoomUser().getPosition();

        // If the pet is already facing this direction, walk one tile that way
        if (pos.getHeadRotation() == headRot && pos.getBodyRotation() == bodyRot) {
            int targetX = pos.getX() + DIRECTION_X[bodyRot];
            int targetY = pos.getY() + DIRECTION_Y[bodyRot];

            pet.getRoomUser().walkTo(targetX, targetY);
            return;
        }

        pet.getRoomUser().getPosition().setHeadRotation(headRot);
        pet.getRoomUser().getPosition().setBodyRotation(bodyRot);
        pet.getRoomUser().setNeedsUpdate(true);
    }

    @Override
    public String getDescription() {
        return "Sets a pet's facing direction, or walks one tile if already facing that way (used by pet control panel)";
    }
}
