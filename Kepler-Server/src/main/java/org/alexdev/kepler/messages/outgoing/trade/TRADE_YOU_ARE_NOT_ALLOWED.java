package org.alexdev.kepler.messages.outgoing.trade;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * You may not trade (trade ban): text trade_youarenotallowed.
 */
public class TRADE_YOU_ARE_NOT_ALLOWED extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {

    }

    @Override
    public short getHeader() {
        return 102;
    }
}
