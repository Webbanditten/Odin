package org.alexdev.kepler.messages.outgoing.trade;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * The other player can't trade (trade ban, or already trading): text trade_othernotallowed.
 */
public class TRADE_OTHER_NOT_ALLOWED extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {

    }

    @Override
    public short getHeader() {
        return 103;
    }
}
