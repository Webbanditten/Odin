package org.alexdev.kepler.messages.outgoing.songs;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * The sound machine holds no more Trax (setting soundmachine.songs.max): "Traxmachine kan kun indeholde %count% Trax." The client closes the editor of a new Trax.
 */
public class SONG_LIST_FULL extends MessageComposer {
    private final int max;

    public SONG_LIST_FULL(int max) {
        this.max = max;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.max);
    }

    @Override
    public short getHeader() {
        return 326;
    }
}
