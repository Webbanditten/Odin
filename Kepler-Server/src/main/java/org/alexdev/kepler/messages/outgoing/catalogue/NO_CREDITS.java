package org.alexdev.kepler.messages.outgoing.catalogue;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class NO_CREDITS extends MessageComposer {
    private final boolean newCatalogue;

    public NO_CREDITS() {
        this(false);
    }

    /** @param newCatalogue the client runs the v31 catalogue, which reads the missing currencies */
    public NO_CREDITS(boolean newCatalogue) {
        this.newCatalogue = newCatalogue;
    }

    @Override
    public void compose(NettyResponse response) {
        if (this.newCatalogue) {
            // The v31 catalogue reads what is missing: credits, pixels.
            response.writeInt(1);
            response.writeInt(0);
        }
    }

    @Override
    public short getHeader() {
        return 68; // "AD"
    }
}
