package org.alexdev.kepler.game.commands.registered;

import org.alexdev.kepler.game.commands.Command;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.programs.RoomProgramTask;
import org.alexdev.kepler.messages.outgoing.alert.ALERT;
import org.alexdev.kepler.messages.outgoing.rooms.items.SHOWPROGRAM;

/**
 * :program &lt;dest&gt; &lt;command&gt; [param] sends a SHOWPROGRAM to the room you're in (for events: "discofloor 9" in
 * Club Mammoth, "ambient1 fade 003300" in Club 57's dance floor ...). In a room with a running show the channel then
 * stays like that until ":program auto". ":program" alone shows the room's current state. Reference:
 * Odin/docs/room-programs.md.
 */
public class ProgramCommand extends Command {
    @Override
    public void addPermissions() {
        this.permissions.add(Fuse.ROOM_ALERT);
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

        RoomProgramTask task = room.getTaskManager().getTask(RoomProgramTask.TASK_NAME) instanceof RoomProgramTask t ? t : null;

        if (args.length == 0 || args[0].isBlank()) {
            String state = task == null ? "This room has no automatic show." : "Now: " + String.join(", ", task.describe());
            player.send(new ALERT(state + "<br><br>:program &lt;dest&gt; &lt;command&gt; [param] sets it, :program auto lets the timer take over again. "
                    + "The rooms and their values: Odin/docs/room-programs.md"));
            return;
        }

        if (args[0].equalsIgnoreCase("auto")) {
            if (task != null) {
                task.resume();
            }

            player.send(new ALERT(task == null ? "This room has no automatic show." : "The room's show runs by itself again."));
            return;
        }

        if (args.length < 2) {
            player.send(new ALERT("Usage: :program <dest> <command> [param], e.g. :program discofloor 4"));
            return;
        }

        // The client keeps only the first three words of a SHOWPROGRAM.
        String[] show = args.length >= 3 ? new String[] { args[0], args[1], args[2] } : new String[] { args[0], args[1] };
        room.send(new SHOWPROGRAM(show));

        if (task != null) {
            task.hold(args[0], show);
        }
    }

    @Override
    public String getDescription() {
        return "Change a public room's show (disco floor, lights): :program discofloor 4 | :program auto | :program";
    }
}
