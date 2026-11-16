package org.alexdev.kepler.messages.incoming.handshake;

import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.game.GameScheduler;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.user.HOTEL_LOGOUT;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.util.concurrent.TimeUnit;

public class SSO implements MessageEvent {

    @Override
    public void handle(Player player, NettyRequest reader) {
        if (player.isLoggedIn()) {
            return;
        }

        String ticket = reader.readString();

        if (!PlayerDao.loginTicket(player, ticket)) {
            // Unknown or already used ticket (an old /client tab, an expired session). Kicking showed
            // "connection lost". A "Login incorrect" error would show the client's login form, but the client
            // keeps the stale ticket and sends it again instead of the password, then opens the
            // forgotten-password page. A timeout logout makes the client open url_logout_timeout (the CMS's
            // /account/disconnected), which sends the user back to /client: a fresh ticket when they are
            // logged in on the website, the website's normal login when not.
            // Close the socket shortly after, so one connection can't be used to try ticket after ticket.
            player.send(new HOTEL_LOGOUT(HOTEL_LOGOUT.LogoutReason.LOGOUT_TIMEOUT));
            GameScheduler.getInstance().getService().schedule(() -> {
                if (!player.isLoggedIn()) {
                    player.getNetwork().disconnect();
                }
            }, 2, TimeUnit.SECONDS);
        } else {
            player.login();
        }
    }
}
