package org.alexdev.kepler.messages.outgoing.rooms.user;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * Answer to GET_SPECTATOR_AMOUNT: the "tilskuere %cnt%/%max%" counter on the spectator's TV screen. A count of -1
 * removes the counter.
 */
public class SPECTATOR_AMOUNT extends MessageComposer {
    private final int count;
    private final int max;

    public SPECTATOR_AMOUNT(int count, int max) {
        this.count = count;
        this.max = max;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.count);
        response.writeInt(this.max);
    }

    @Override
    public short getHeader() {
        return 298;
    }
}
