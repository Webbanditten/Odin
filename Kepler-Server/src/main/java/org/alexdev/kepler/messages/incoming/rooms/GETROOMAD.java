package org.alexdev.kepler.messages.incoming.rooms;

import org.alexdev.kepler.game.ads.AdManager;
import org.alexdev.kepler.game.ads.Advertisement;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.rooms.ROOMAD;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

/**
 * The client asks for the billboard of the public room it entered (hh_room_utils "Ad Manager"). It downloads the picture,
 * shows it in the room's frame and opens the click URL when the billboard is clicked. Ads come from Housekeeping > Ads.
 */
public class GETROOMAD implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        Room room = player.getRoomUser().getRoom();

        if (room == null) {
            return;
        }

        String baseUrl = AdManager.getBaseUrl();

        // The room photographer takes pictures of the room itself, without ads.
        if (baseUrl == null || !room.isPublicRoom() || player.isPhotographer()) {
            player.send(new ROOMAD(null, null));
            return;
        }

        AdManager ads = AdManager.getInstance();
        Advertisement ad = ads.pickBillboard(room.getId());

        if (ad == null) {
            player.send(new ROOMAD(null, null));
            return;
        }

        player.send(new ROOMAD(AdManager.getImageUrl(baseUrl, ad, ads.getBillboardFrame(room.getId())), AdManager.getClickUrl(baseUrl, ad)));
    }
}
