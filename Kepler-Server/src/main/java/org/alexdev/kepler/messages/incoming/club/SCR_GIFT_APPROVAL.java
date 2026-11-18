package org.alexdev.kepler.messages.incoming.club;

import org.alexdev.kepler.game.club.ClubSubscription;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.sql.SQLException;

public class SCR_GIFT_APPROVAL implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        // The client sends how many gifts the player accepted in its gift dialogs (CLUB_GIFT announced how many are due).
        int accepted = 1;

        if (reader.remainingBytes().length > 0) {
            accepted = Math.max(1, reader.readInt());
        }

        if (ClubSubscription.isGiftDue(player)) {
            try {
                ClubSubscription.giveGifts(player, accepted);
            } catch (SQLException e) {
                Log.getErrorLogger().error("Error trying to process club gift for user (" + player.getDetails().getName() + "): ", e);
            }
        }
    }
}
