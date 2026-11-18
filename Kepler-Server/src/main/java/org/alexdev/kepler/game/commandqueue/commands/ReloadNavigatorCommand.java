package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.navigator.NavigatorManager;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.outgoing.rooms.TRADING_STATUS_UPDATE;

import java.util.HashMap;
import java.util.Map;

public class ReloadNavigatorCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        Map<Integer, Boolean> tradingBefore = new HashMap<>();
        for (Room room : RoomManager.getInstance().getRooms()) {
            tradingBefore.put(room.getId(), room.getCategory() != null && room.getCategory().hasAllowTrading());
        }

        // Re-sync cached public room data (name/description/ccts) from the database so
        // campaign changes to these fields take effect without a server restart. Rooms
        // not yet cached are loaded fresh from the DB on next access, so only the
        // already-loaded public rooms need refreshing here.
        for (Room room : RoomManager.getInstance().getRooms()) {
            if (!room.isPublicRoom()) {
                // Housekeeping > Room categories moves the rooms of a deleted category.
                Room fresh = RoomDao.getRoomById(room.getId());
                if (fresh != null) {
                    room.getData().setCategoryId(fresh.getData().getCategoryId());
                }

                continue;
            }

            Room fresh = RoomDao.getRoomById(room.getId());
            if (fresh != null) {
                room.getData().setName(fresh.getData().getName());
                room.getData().setDescription(fresh.getData().getDescription());
                room.getData().setCcts(fresh.getData().getCcts());
                // Housekeeping > Public rooms: category, place in it and whether the navigator shows it.
                room.getData().setCategoryId(fresh.getData().getCategoryId());
                room.getData().setNavigatorOrder(fresh.getData().getNavigatorOrder());
                room.getData().setHidden(fresh.getData().isHidden());
            }
        }

        NavigatorManager.getInstance().resetCategoryMap();

        // Players in a room whose category now allows trading, or no longer does, get the new trading status.
        for (Room room : RoomManager.getInstance().getRooms()) {
            boolean trading = room.getCategory() != null && room.getCategory().hasAllowTrading();
            if (tradingBefore.containsKey(room.getId()) && tradingBefore.get(room.getId()) != trading) {
                room.send(new TRADING_STATUS_UPDATE(trading ? 1 : 0));
            }
        }
    }
}
