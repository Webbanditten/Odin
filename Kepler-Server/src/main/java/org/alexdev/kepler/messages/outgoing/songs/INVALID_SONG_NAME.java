package org.alexdev.kepler.messages.outgoing.songs;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * Burning a Trax with the name of another Trax the player already burned: "Du har allerede brændt et andet Trax med det samme navn."
 */
public class INVALID_SONG_NAME extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
    }

    @Override
    public short getHeader() {
        return 332;
    }
}
