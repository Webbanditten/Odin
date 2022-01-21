package org.alexdev.kepler.messages.outgoing.poll;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class POLL_OFFER extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {
        response.writeInt(1);
        response.writeString("TEST");
    }

    @Override
    public short getHeader() {
        return 316;
    }
}