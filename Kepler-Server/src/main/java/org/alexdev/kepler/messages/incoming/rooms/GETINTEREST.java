package org.alexdev.kepler.messages.incoming.rooms;

import org.alexdev.kepler.game.ads.AdManager;
import org.alexdev.kepler.game.ads.Advertisement;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.rooms.INTERSITIALDATA;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.util.config.GameConfiguration;

/**
 * Sent ("general") on every room entry; the room waits for the answer. With an ad, the client shows it in the loading
 * window for 4 seconds (at most interstitial.max.displays = 5 times per visit). Odin answers with one every
 * ads.loading.every-th room entry (0 = never).
 */
public class GETINTEREST implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        String baseUrl = AdManager.getBaseUrl();
        int every = GameConfiguration.getInstance().getInteger("ads.loading.every");
        int entry = player.countRoomEntryForAds();

        if (baseUrl == null || every <= 0 || entry % every != 0 || player.isPhotographer()) {
            player.send(new INTERSITIALDATA(null, null));
            return;
        }

        Advertisement ad = AdManager.getInstance().pick(AdManager.ROOM_LOADING);

        if (ad == null) {
            player.send(new INTERSITIALDATA(null, null));
            return;
        }

        player.send(new INTERSITIALDATA(AdManager.getImageUrl(baseUrl, ad, null), AdManager.getClickUrl(baseUrl, ad)));
    }
}
