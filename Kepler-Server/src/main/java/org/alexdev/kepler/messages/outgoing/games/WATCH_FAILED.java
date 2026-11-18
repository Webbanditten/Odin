package org.alexdev.kepler.messages.outgoing.games;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * Watching a game failed; reason 0 = all spectator seats are taken ("Alle tilskuerpladserne er optagede!", setting games.spectators.max).
 */
public class WATCH_FAILED extends MessageComposer {
    private final int instanceId;
    private final int reason;

    public WATCH_FAILED(int instanceId, int reason) {
        this.instanceId = instanceId;
        this.reason = reason;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.instanceId);
        response.writeInt(this.reason);
    }

    @Override
    public short getHeader() {
        return 240;
    }
}
