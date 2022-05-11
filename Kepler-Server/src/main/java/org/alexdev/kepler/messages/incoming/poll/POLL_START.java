package org.alexdev.kepler.messages.incoming.poll;

import org.alexdev.kepler.dao.mysql.PollDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.polls.Poll;
import org.alexdev.kepler.messages.outgoing.poll.POLL_CONTENTS;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.sql.SQLException;

public class POLL_START implements MessageEvent {

    @Override
    public void handle(Player player, NettyRequest reader) throws SQLException {
        if (!player.isLoggedIn()) {
            return;
        }

        Integer id = reader.readInt();
        PollDao.addOffer(id, player.getDetails().getId());
        Poll poll = PollDao.getPoll(id);
        player.send(new POLL_CONTENTS(poll));

    }
}
