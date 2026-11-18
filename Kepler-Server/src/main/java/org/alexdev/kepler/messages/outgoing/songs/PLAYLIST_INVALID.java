package org.alexdev.kepler.messages.outgoing.songs;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * The sound machine playlist can't take that many Trax: "Playlister kan kun indeholde %count% Trax."
 */
public class PLAYLIST_INVALID extends MessageComposer {
    private final int max;

    public PLAYLIST_INVALID(int max) {
        this.max = max;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.max);
    }

    @Override
    public short getHeader() {
        return 325;
    }
}
