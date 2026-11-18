package org.alexdev.kepler.messages.outgoing.rooms;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class ROOMAD extends MessageComposer {
    private final String img;
    private final String url;
    public ROOMAD() {
        this.img = null;
        this.url = null;
    }
    public ROOMAD(String img, String url) {
        this.img = img;
        this.url = url;
    }
    @Override
    public void compose(NettyResponse response) {
        // The client reads the body as "<picture URL><TAB><click URL>" (items of the whole message), so no string
        // terminator; anything of one character or less means "no ad".
        if (this.img != null) {
            response.write(this.img + "\t" + (this.url != null ? this.url : ""));
        } else {
            response.writeInt(0);
        }
    }

    @Override
    public short getHeader() {
        return 208; // "CP"
    }
}
