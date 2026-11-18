package org.alexdev.kepler.messages.incoming.navigator;

import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.game.room.RoomRatings;
import org.alexdev.kepler.messages.outgoing.navigator.RECOMMENDED_ROOM_LIST;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.util.List;

/**
 * "Anbefalede Rum" in the navigator: the week's best rated open rooms (RoomRatings: votes cast in the last 7 days), then
 * the best rated ones of all time when fewer rooms got votes this week.
 */
public class RECOMMENDED_ROOMS implements MessageEvent {
    private static final int ROOM_LIMIT = 3;

    @Override
    public void handle(Player player, NettyRequest reader) {
        List<Room> roomList = RoomManager.getInstance().replaceQueryRooms(RoomRatings.getInstance().getRecommended(ROOM_LIMIT));

        if (roomList.size() < ROOM_LIMIT) {
            for (Room room : RoomManager.getInstance().replaceQueryRooms(RoomDao.getHighestRatedRooms(ROOM_LIMIT * 2, false))) {
                if (roomList.size() == ROOM_LIMIT) {
                    break;
                }

                if (roomList.stream().noneMatch(r -> r.getId() == room.getId())) {
                    roomList.add(room);
                }
            }
        }

        player.send(new RECOMMENDED_ROOM_LIST(player, roomList));
    }
}
