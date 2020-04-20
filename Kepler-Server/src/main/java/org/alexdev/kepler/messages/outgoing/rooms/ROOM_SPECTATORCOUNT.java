package org.alexdev.kepler.messages.outgoing.rooms;

import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;
import org.alexdev.kepler.util.encoding.VL64Encoding;

public class ROOM_SPECTATORCOUNT extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
        String minSpectators = new String(VL64Encoding.encode(1));
        String maxSpectators = new String(VL64Encoding.encode(100));

        response.writeString(minSpectators + maxSpectators);
    }

    @Override
    public short getHeader() {
        return 298; // "Dj"
    }
}
