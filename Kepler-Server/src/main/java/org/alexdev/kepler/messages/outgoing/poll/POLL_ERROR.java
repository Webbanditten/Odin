package org.alexdev.kepler.messages.outgoing.poll;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class POLL_ERROR extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
        // No body - the client just shows a generic error dialog
    }

    @Override
    public short getHeader() {
        return 318;
    }
}
