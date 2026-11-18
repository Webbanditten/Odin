package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.navigator.NavigatorManager;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomData;
import org.alexdev.kepler.game.room.RoomManager;

/**
 * Housekeeping > Rooms saved a room (the CMS has already written the row): the room as loaded in the game gets the
 * new name, description, owner display, access, queue length and spectator seats at once. A shorter queue or fewer
 * seats take effect at the next queue round (RoomQueueManager.process).
 */
public class RoomUpdateCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        var room = RoomDao.getRoomById(commandArgs.RoomId);
        if(room != null) {
            apply(room.getData(), commandArgs);
            RoomDao.save(room);

            if (RoomManager.getInstance().hasRoom(commandArgs.RoomId)) {
                Room loaded = RoomManager.getInstance().getRoomById(commandArgs.RoomId);
                apply(loaded.getData(), commandArgs);
            }

            NavigatorManager.getInstance().resetCategoryMap();
        }
    }

    private static void apply(RoomData data, CommandTemplate commandArgs) {
        data.setName(commandArgs.RoomName);
        data.setDescription(commandArgs.RoomDescription);
        data.setShowOwnerName(commandArgs.RoomShowOwnerName);
        data.setAccessType(commandArgs.RoomAccessType);

        // Absent when an older CMS sends the command: keep what the room has.
        if (commandArgs.RoomQueueMax != null) {
            data.setQueueMax(commandArgs.RoomQueueMax);
        }

        if (commandArgs.RoomSpectatorsMax != null) {
            data.setSpectatorsMax(commandArgs.RoomSpectatorsMax);
        }
    }
}
