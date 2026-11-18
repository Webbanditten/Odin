package org.alexdev.kepler.messages.outgoing.songs;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * Burning an empty Trax: "Du kan ikke brænde et tomt Trax!"
 */
public class INVALID_SONG_LENGTH extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
    }

    @Override
    public short getHeader() {
        return 338;
    }
}
