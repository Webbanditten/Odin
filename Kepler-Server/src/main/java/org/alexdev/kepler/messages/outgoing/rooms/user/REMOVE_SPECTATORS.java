package org.alexdev.kepler.messages.outgoing.rooms.user;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * Tells a spectator their turn is over (text room_remove_specs: "Beklager! Nu er det en andens tur til at se eventet.").
 */
public class REMOVE_SPECTATORS extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {

    }

    @Override
    public short getHeader() {
        return 283;
    }
}
