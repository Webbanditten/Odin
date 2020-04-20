package org.alexdev.kepler.messages.outgoing.rooms;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class ROOM_INTEREST extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {
        response.writeInt(0);
        //response.writeString("http://dcr.webbanditten.dk/v14/c_images/ads/test-icon-png-1.png\thttp://google.com\r");
        //response.writeDelimeter("https://classichabbo.com/api/advertisement/get_img?ad=73", (char) 9);
        //response.writeDelimeter("http://google.com/", (char) 13);
    }

    @Override
    public short getHeader() {
        return 258; // (DB) Outgoing.ROOM_INTEREST;
    }
}
