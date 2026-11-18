package org.alexdev.kepler.messages.incoming.navigator;

import org.alexdev.kepler.dao.mysql.NavigatorDao;
import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.navigator.NavigatorCategory;
import org.alexdev.kepler.game.navigator.NavigatorManager;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.outgoing.messenger.ROOMFORWARD;
import org.alexdev.kepler.messages.outgoing.navigator.NAVNODEINFO;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NAVIGATE implements MessageEvent {
    /** The client's navigator.public.default: "Fællesrum", the root of the public room categories. */
    private static final int PUBLIC_ROOT_CATEGORY = 3;

    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        boolean hideFull = reader.readInt() == 1;
        int categoryId = reader.readInt();

        boolean wasFollow = false;
        int originalCategoryId = categoryId;

        if (categoryId >= RoomManager.PUBLIC_ROOM_OFFSET) { // Public room follow, there should not any categories with an ID of 1000 or over... lol
            Room room = RoomManager.getInstance().getRoomById(categoryId - RoomManager.PUBLIC_ROOM_OFFSET);

            if (room != null) {
                wasFollow = true;
                // A public room whose category doesn't exist (Habbo Poolen II's 10, the walkway parts' 7, ...) opens the
                // public root instead; getCategory() returned null here and the follow threw.
                categoryId = room.getCategory() != null ? room.getCategory().getId() : PUBLIC_ROOT_CATEGORY;
                player.send(new ROOMFORWARD(true, room.getData().getId()));
            }
        }

        NavigatorCategory category = NavigatorManager.getInstance().getCategoryById(categoryId);

        if (category == null) {
            return;
        }

        boolean hasFuse = player.getDetails().getFuseRights().stream().anyMatch(r -> r.getFuse().equalsIgnoreCase(category.getFuseAccess()));
        if (!hasFuse) {
            return;
        }

        List<NavigatorCategory> subCategories = NavigatorManager.getInstance().getCategoriesByParentId(category.getId());
        Comparator<NavigatorCategory> busiest = Comparator.comparingInt(NavigatorCategory::getCurrentVisitors).reversed();

        if (category.isPublicSpaces()) {
            // Housekeeping > Public rooms orders the public categories (order_id); busiest first unless switched off.
            Comparator<NavigatorCategory> arranged = Comparator.comparingInt(NavigatorCategory::getOrderId).thenComparingInt(NavigatorCategory::getId);
            subCategories.sort(GameConfiguration.getInstance().getBoolean("navigator.public.busiest.first") ? busiest.thenComparing(arranged) : arranged);
        } else {
            subCategories.sort(busiest.thenComparingInt(NavigatorCategory::getId)); // guest room categories: as before
        }

        List<Room> rooms = new ArrayList<>();

        int categoryCurrentVisitors = category.getCurrentVisitors();
        int categoryMaxVisitors = category.getMaxVisitors();

        if (category.isPublicSpaces()) {
            for (Room room : RoomManager.getInstance().replaceQueryRooms(RoomDao.getRoomsByUserId(0))) {
                if (room.getData().isNavigatorHide()) {
                    continue;
                }

                if (room.getData().getCategoryId() != category.getId()) {
                    continue;
                }

                if (hideFull && (room.getData().getVisitorsNow() >= room.getData().getVisitorsMax())) {
                    continue;
                }

                rooms.add(room);
            }
        } else {
            for (Room room : RoomManager.getInstance().replaceQueryRooms(NavigatorDao.getRecentRooms(30, category.getId()))) {
                if (room.getData().getCategoryId() != category.getId()) {
                    continue;
                }

                if (hideFull && (room.getData().getVisitorsNow() >= room.getData().getVisitorsMax())) {
                    continue;
                }

                rooms.add(room);
            }
        }

        if (category.isPublicSpaces()) {
            RoomManager.getInstance().sortPublicRooms(rooms);
        } else {
            RoomManager.getInstance().sortRooms(rooms);
        }

        RoomManager.getInstance().ratingSantiyCheck(rooms);

        player.send(new NAVNODEINFO(player, category, rooms, hideFull, subCategories, categoryCurrentVisitors, categoryMaxVisitors, player.getDetails().getFuseRights()));

        if (wasFollow && player.getMessenger().getFollowed() != null) {
            player.getMessenger().getFollowed().forward(player, false);
            player.getMessenger().hasFollowed(null);
        }
    }
}
