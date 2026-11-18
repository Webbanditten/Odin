package org.alexdev.kepler.messages.incoming.rooms;

import org.alexdev.kepler.game.ads.AdManager;
import org.alexdev.kepler.game.ads.Advertisement;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.rooms.INTERSITIALDATA;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sent on every room entry, before the room itself (which waits for the answer). With an ad, the client shows it in the
 * loading window for 4 seconds (at most interstitial.max.displays = 5 times per visit).
 *
 * The original client sends "general". The browser client's hh_room_ads (prod-staging/ads/patch-room-interstitial.cjs)
 * sends the room it enters instead, as Lingo prints the room struct: [#id: "1018", #type: #private, ...] for a private
 * room, [#id: "<unit>", #type: #public, #port: 3, ...] for a public one (#port is the room id). An ad limited to rooms
 * (Housekeeping > Ads) is shown on every entry into them; ads without a room limit every ads.loading.every-th room
 * entry (0 = never).
 */
public class GETINTEREST implements MessageEvent {
    private static final Pattern TYPE = Pattern.compile("#type: #(\\w+)");
    private static final Pattern PORT = Pattern.compile("#port: \"?(\\d+)");
    private static final Pattern FLAT_ID = Pattern.compile("#flatId: \"?(\\d+)");
    private static final Pattern ID = Pattern.compile("#id: \"?(?:f_)?(\\d+)");

    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        String baseUrl = AdManager.getBaseUrl();
        int every = GameConfiguration.getInstance().getInteger("ads.loading.every");
        int entry = player.countRoomEntryForAds();

        if (baseUrl == null || player.isPhotographer()) {
            player.send(new INTERSITIALDATA(null, null));
            return;
        }

        int roomId = roomOf(reader.contents());
        Advertisement ad = roomId > 0 ? AdManager.getInstance().pickLimitedTo(AdManager.ROOM_LOADING, roomId) : null;

        if (ad == null && every > 0 && entry % every == 0) {
            ad = AdManager.getInstance().pick(AdManager.ROOM_LOADING);
        }

        if (ad == null) {
            player.send(new INTERSITIALDATA(null, null));
            return;
        }

        player.send(new INTERSITIALDATA(AdManager.getImageUrl(baseUrl, ad, null), AdManager.getClickUrl(baseUrl, ad)));
    }

    /** The room id in the client's room struct, or 0 ("general", or nothing to go by). */
    public static int roomOf(String body) {
        if (body == null || !body.startsWith("[")) {
            return 0;
        }

        String type = first(TYPE, body);

        if (type == null) {
            return 0;
        }

        String id = type.equalsIgnoreCase("public") ? first(PORT, body) : first(FLAT_ID, body);

        if (id == null && !type.equalsIgnoreCase("public")) {
            id = first(ID, body);
        }

        try {
            return id != null ? Integer.parseInt(id) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String first(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }
}
