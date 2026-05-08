package org.alexdev.kepler.messages.incoming.poll;

import org.alexdev.kepler.dao.mysql.PollDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.sql.SQLException;

public class POLL_REJECT implements MessageEvent {

    @Override
    public void handle(Player player, NettyRequest reader) throws SQLException {
        if (!player.isLoggedIn()) {
            return;
        }

        int pollId = reader.readInt();

        // Record that this user rejected the poll offer (prevents re-offering)
        PollDao.addOffer(pollId, player.getDetails().getId(), "REJECTED");
    }
}
