package org.alexdev.kepler.messages.outgoing.games;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * The game a player wants to watch, join or look at is gone: "Spillet er blevet slettet."
 */
public class INSTANCE_NOT_AVAILABLE extends MessageComposer {
    private final int instanceId;

    public INSTANCE_NOT_AVAILABLE(int instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.instanceId);
    }

    @Override
    public short getHeader() {
        return 234;
    }
}
