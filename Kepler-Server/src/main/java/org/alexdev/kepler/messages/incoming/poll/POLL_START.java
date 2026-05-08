package org.alexdev.kepler.messages.incoming.poll;

import org.alexdev.kepler.dao.mysql.PollDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.polls.Poll;
import org.alexdev.kepler.messages.outgoing.poll.POLL_CONTENTS;
import org.alexdev.kepler.messages.outgoing.poll.POLL_ERROR;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.sql.SQLException;

public class POLL_START implements MessageEvent {

    @Override
    public void handle(Player player, NettyRequest reader) throws SQLException {
        if (!player.isLoggedIn()) {
            return;
        }

        int pollId = reader.readInt();

        Poll poll = PollDao.getPoll(pollId);
        if (poll == null) {
            player.send(new POLL_ERROR());
            return;
        }

        // Record that this user accepted the poll offer
        PollDao.addOffer(pollId, player.getDetails().getId(), "ACCEPTED");

        player.send(new POLL_CONTENTS(poll));
    }
}
