package org.alexdev.kepler.game.navigator;

import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.game.room.RoomRatings;

public class NavigatorCategory {
    private int id;
    private int parentId;
    private String name;
    private boolean publicSpaces;
    private boolean allowTrading;
    private String fuseAccess;
    private String fuseSetFlat;
    private boolean isNode;
    private int orderId;
    private String ranking;

    public NavigatorCategory(int id, int parentId, String name, boolean publicSpaces, boolean allowTrading, String fuseAccess, String fuseSetFlat, boolean isNode, int orderId, String ranking) {
        this.id = id;
        this.orderId = orderId;
        this.ranking = ranking == null ? "" : ranking.trim();
        this.parentId = parentId;
        this.name = name;
        this.publicSpaces = publicSpaces;
        this.allowTrading = allowTrading;
        this.fuseAccess = fuseAccess;
        this.fuseSetFlat = fuseSetFlat;
        this.isNode = isNode;
    }

    /**
     * A ranking category (rooms_categories.ranking = daily, weekly or alltime) lists the best rated guest rooms instead of
     * the rooms in it (RoomRatings); no room can be put in it.
     */
    public boolean isRanking() {
        return !this.ranking.isEmpty();
    }

    public String getRanking() {
        return ranking;
    }

    public int getCurrentVisitors() {
        if (this.isRanking()) {
            return RoomManager.getInstance().replaceQueryRooms(RoomRatings.getInstance().getRooms(this.ranking)).stream()
                    .mapToInt(room -> room.getData().getVisitorsNow()).sum();
        }

        int currentVisitors = 0;

        for (Room room : RoomManager.getInstance().getRooms()) {
            if (room.getData().getCategoryId() == this.id) {
                currentVisitors += room.getData().getVisitorsNow();
            }
        }

        return currentVisitors;
    }

    public int getMaxVisitors() {
        if (this.isRanking()) {
            return RoomRatings.getInstance().getRooms(this.ranking).stream().mapToInt(room -> room.getData().getVisitorsMax()).sum();
        }

        int maxVisitors = 0;

        for (Room room : RoomManager.getInstance().getRooms()) {
            if (room.getData().getCategoryId() == this.id) {
                maxVisitors += room.getData().getVisitorsMax();
            }
        }

        return maxVisitors;
    }

    public int getId() {
        return id;
    }

    /**
     * The category's place among its siblings (rooms_categories.order_id), set in Housekeeping > Public rooms.
     */
    public int getOrderId() {
        return orderId;
    }

    public int getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public boolean isPublicSpaces() {
        return publicSpaces;
    }

    public boolean hasAllowTrading() {
        return allowTrading;
    }

    public String getFuseSetFlat() {
        return fuseSetFlat;
    }

    public String getFuseAccess() {
        return fuseAccess;
    }

    public boolean isNode() {
        return isNode;
    }
}
