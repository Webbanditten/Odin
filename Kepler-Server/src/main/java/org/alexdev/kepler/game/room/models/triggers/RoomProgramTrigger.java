package org.alexdev.kepler.game.room.models.triggers;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.programs.RoomProgramTask;
import org.alexdev.kepler.game.room.programs.RoomShows;
import org.alexdev.kepler.game.triggers.GenericTrigger;
import org.alexdev.kepler.messages.outgoing.rooms.items.SHOWPROGRAM;

import java.util.concurrent.TimeUnit;

/**
 * Public rooms whose shows the server changes (rooms_models.trigger_class = room_program_trigger; docs/room-programs.md):
 * the first visitor starts the room's {@link RoomProgramTask}, and everyone who walks in gets the current state.
 */
public class RoomProgramTrigger extends GenericTrigger {
    @Override
    public void onRoomEntry(Entity entity, Room room, boolean firstEntry, Object... customArgs) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (!room.getTaskManager().hasTask(RoomProgramTask.TASK_NAME)) {
            RoomProgramTask task = RoomProgramTask.forRoom(room);

            if (task != null) {
                room.getTaskManager().scheduleTask(RoomProgramTask.TASK_NAME, task, 500, 500, TimeUnit.MILLISECONDS);
            }
        }

        sendCurrentShows(player, room);
    }

    /** The room's current shows to one player (a late joiner would otherwise see the client's defaults). */
    public static void sendCurrentShows(Player player, Room room) {
        RoomShows shows = RoomShows.of(room);

        if (shows == null) {
            return;
        }

        for (String[] show : shows.currentShows()) {
            player.send(new SHOWPROGRAM(show));
        }
    }

    @Override
    public void onRoomLeave(Entity entity, Room room, Object... customArgs) {

    }
}
