package org.alexdev.kepler.messages.outgoing.poll;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class POLL_OFFER extends MessageComposer {

    private final Integer id;
    private final String name;

    public POLL_OFFER(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.id);
        response.writeString(this.name);
    }

    @Override
    public short getHeader() {
        return 316;
    }
}