package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.navigator.NavigatorManager;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;

public class ReloadNavigatorCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        // Re-sync cached public room data (name/description/ccts) from the database so
        // campaign changes to these fields take effect without a server restart. Rooms
        // not yet cached are loaded fresh from the DB on next access, so only the
        // already-loaded public rooms need refreshing here.
        for (Room room : RoomManager.getInstance().getRooms()) {
            if (!room.isPublicRoom()) {
                continue;
            }

            Room fresh = RoomDao.getRoomById(room.getId());
            if (fresh != null) {
                room.getData().setName(fresh.getData().getName());
                room.getData().setDescription(fresh.getData().getDescription());
                room.getData().setCcts(fresh.getData().getCcts());
            }
        }

        NavigatorManager.getInstance().resetCategoryMap();
    }
}
