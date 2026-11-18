package org.alexdev.kepler.messages.outgoing.songs;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * Saving changes to a burned Trax under the same name (its discs play it): "Gem Trax'et under et andet navn for at oprette en ny kopi."
 */
public class SONG_LOCKED extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
    }

    @Override
    public short getHeader() {
        return 336;
    }
}
