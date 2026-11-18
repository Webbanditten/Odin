package org.alexdev.kepler.messages.outgoing.games;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * In a game lobby a minute before the AFK kick: "Du bliver sparket ud, hvis du ikke starter eller hopper med på et spil snart!"
 */
public class IDLE_WARNING extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
    }

    @Override
    public short getHeader() {
        return 251;
    }
}
