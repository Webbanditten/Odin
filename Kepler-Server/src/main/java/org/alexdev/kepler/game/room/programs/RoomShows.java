package org.alexdev.kepler.game.room.programs;

import org.alexdev.kepler.game.room.Room;

import java.util.List;

/**
 * A public room's running show (disco floors, lights, flip boards): what a player who walks in now has to be sent so
 * the room looks the same as for everyone already there. The client starts every room in its default state, and only
 * the server's SHOWPROGRAM messages change it. See docs/room-programs.md.
 */
public interface RoomShows {
    /** The SHOWPROGRAM messages ({dest, command, param}) that recreate the current state. */
    List<String[]> currentShows();

    /** The room's running show, if it has one (the task its trigger scheduled). */
    static RoomShows of(Room room) {
        for (String name : new String[] { RoomProgramTask.TASK_NAME, "SpaceCafeTask" }) {
            if (room.getTaskManager().getTask(name) instanceof RoomShows shows) {
                return shows;
            }
        }

        return null;
    }
}
