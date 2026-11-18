package org.alexdev.kepler.messages.outgoing.songs;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * The jukebox playlist is full: "Playlisten er fuld. Vent til den nuværende sang er færdig og prøv igen."
 */
public class JUKEBOX_PLAYLIST_FULL extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
    }

    @Override
    public short getHeader() {
        return 337;
    }
}
