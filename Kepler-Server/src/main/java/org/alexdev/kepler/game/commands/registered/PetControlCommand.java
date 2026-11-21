package org.alexdev.kepler.game.commands.registered;

import org.alexdev.kepler.game.commands.Command;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.enums.StatusType;
import org.alexdev.kepler.game.room.tasks.PetTask;

import java.util.Arrays;

/**
 * The pet "Control" window (fuse_debug_window: the pet's info stand shows a Control button). The client sends:
 * ":passivate &lt;id&gt;" when the window opens (the pet stops doing things on its own), ":activate &lt;id&gt;" when it
 * closes, ":setdir &lt;id&gt; &lt;head&gt; &lt;body&gt;", and ":actionon/:actionoff &lt;id&gt; Dev &lt;status&gt; [value]" to show
 * or clear a pose or face ("snf", "gst sml", "sit 0.0"...). &lt;id&gt; is the pet's room instance id.
 */
public class PetControlCommand extends Command {
    @Override
    public void addPermissions() {
        this.permissions.add(Fuse.DEBUG);
    }

    @Override
    public void addArguments() {
        this.arguments.add("pet");
    }

    @Override
    public void handleCommand(Entity entity, String message, String[] args) {
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) entity;
        Room room = player.getRoomUser().getRoom();

        if (room == null || args.length < 1) {
            return;
        }

        Pet pet;

        try {
            Entity target = room.getEntityManager().getByInstanceId(Integer.parseInt(args[0]));

            if (!(target instanceof Pet)) {
                return;
            }

            pet = (Pet) target;
        } catch (NumberFormatException ex) {
            return;
        }

        PetTask task = pet.getRoomUser().getTask();
        String command = message.substring(1).split(" ")[0].toLowerCase();

        switch (command) {
            case "passivate":
                if (task != null) task.setPassive(true);
                break;
            case "activate":
                if (task != null) task.setPassive(false);
                break;
            case "setdir":
                if (args.length >= 3) {
                    try {
                        pet.getRoomUser().getPosition().setHeadRotation(Math.floorMod(Integer.parseInt(args[1]), 8));
                        pet.getRoomUser().getPosition().setBodyRotation(Math.floorMod(Integer.parseInt(args[2]), 8));
                        pet.getRoomUser().setNeedsUpdate(true);
                    } catch (NumberFormatException ignored) { }
                }
                break;
            case "actionon":
            case "actionoff":
                // <id> Dev <status> [value...]
                String[] action = Arrays.copyOfRange(args, Math.min(args.length, 2), args.length);

                if (action.length == 0) {
                    return;
                }

                StatusType type = Arrays.stream(StatusType.values())
                        .filter(t -> t.getStatusCode().equalsIgnoreCase(action[0]))
                        .findFirst().orElse(null);

                if (type == null) {
                    return;
                }

                if (command.equals("actionon")) {
                    pet.getRoomUser().setStatus(type, String.join(" ", Arrays.copyOfRange(action, 1, action.length)));
                } else {
                    pet.getRoomUser().removeStatus(type);
                }

                pet.getRoomUser().setNeedsUpdate(true);
                break;
            default:
                break;
        }
    }

    @Override
    public String getDescription() {
        return "Pet control window (staff)";
    }
}
