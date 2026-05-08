package org.alexdev.kepler.game.commands.registered;

import org.alexdev.kepler.game.commands.Command;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.enums.StatusType;
import org.alexdev.kepler.messages.outgoing.rooms.user.CHAT_MESSAGE;
import org.alexdev.kepler.util.StringUtil;

public class PetActionOnCommand extends Command {

    @Override
    public void addPermissions() {
        this.permissions.add(Fuse.DEFAULT);
    }

    @Override
    public void addArguments() {
        this.arguments.add("petID");
        this.arguments.add("Dev");
        this.arguments.add("action");
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

        // args[1] is "Dev" (unused), args[2] is the action name
        if (args.length < 3) {
            return;
        }

        String actionName = args[2];

        Entity petEntity = room.getEntityManager().getByInstanceId(petId);

        if (petEntity == null || petEntity.getType() != EntityType.PET) {
            return;
        }

        Pet pet = (Pet) petEntity;

        // Handle "s" as speak — join remaining args as the speech text
        if (actionName.equalsIgnoreCase("s")) {
            if (args.length < 4) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(args[i]);
            }

            pet.getRoomUser().talk(sb.toString(), CHAT_MESSAGE.ChatMessageType.CHAT);
            return;
        }

        StatusType statusType = getStatusTypeByCode(actionName);

        if (statusType == null) {
            return;
        }

        // Stop walking if the pet is moving
        if (pet.getRoomUser().isWalking()) {
            pet.getRoomUser().stopWalking();
        }

        // For sit/lay, use the current Z height as the value
        if (statusType == StatusType.SIT || statusType == StatusType.LAY) {
            pet.getRoomUser().setStatus(statusType, StringUtil.format(pet.getRoomUser().getPosition().getZ()));
        } else {
            pet.getRoomUser().setStatus(statusType, statusType.getStatusCode().toLowerCase());
        }

        pet.getRoomUser().setNeedsUpdate(true);
    }

    /**
     * Look up a StatusType by its status code string.
     *
     * @param code the status code (e.g. "sit", "lay", "ded", "jmp")
     * @return the matching StatusType, or null if not found
     */
    private StatusType getStatusTypeByCode(String code) {
        for (StatusType st : StatusType.values()) {
            if (st.getStatusCode().equalsIgnoreCase(code)) {
                return st;
            }
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "Applies a status to a pet (used by pet control panel)";
    }
}
