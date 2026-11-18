package org.alexdev.kepler.game.navigator;

import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;

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

    public NavigatorCategory(int id, int parentId, String name, boolean publicSpaces, boolean allowTrading, String fuseAccess, String fuseSetFlat, boolean isNode, int orderId) {
        this.id = id;
        this.orderId = orderId;
        this.parentId = parentId;
        this.name = name;
        this.publicSpaces = publicSpaces;
        this.allowTrading = allowTrading;
        this.fuseAccess = fuseAccess;
        this.fuseSetFlat = fuseSetFlat;
        this.isNode = isNode;
    }

    public int getCurrentVisitors() {
        int currentVisitors = 0;

        for (Room room : RoomManager.getInstance().getRooms()) {
            if (room.getData().getCategoryId() == this.id) {
                currentVisitors += room.getData().getVisitorsNow();
            }
        }

        return currentVisitors;
    }

    public int getMaxVisitors() {
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
