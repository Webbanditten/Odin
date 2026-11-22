package org.alexdev.kepler.messages.incoming.trade;

import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.managers.RoomTradeManager;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.apache.commons.lang3.StringUtils;

public class TRADE_ADDITEM implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        Room room = player.getRoomUser().getRoom();

        if (room == null) {
            return;
        }

        if (player.getRoomUser().getTradePartner() == null) {
            return;
        }

        // One item id, or several separated by spaces: the web client's hand offers a whole stack at once
        // (Shift-click). Items already offered are skipped; the windows refresh once.
        boolean added = false;

        for (String part : reader.contents().trim().split("[ ,]+")) {
            if (!StringUtils.isNumeric(part)) {
                continue;
            }

            Item inventoryItem = player.getInventory().getItem(Integer.parseInt(part));

            if (inventoryItem == null || player.getRoomUser().getTradeItems().contains(inventoryItem)) {
                continue;
            }

            player.getRoomUser().getTradeItems().add(inventoryItem);
            added = true;
        }

        if (!added) {
            return;
        }

        RoomTradeManager.refreshWindow(player);
        RoomTradeManager.refreshWindow(player.getRoomUser().getTradePartner());
    }
}
