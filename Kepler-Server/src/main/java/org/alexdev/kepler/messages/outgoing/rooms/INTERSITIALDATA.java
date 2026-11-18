
package org.alexdev.kepler.messages.outgoing.rooms;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;
public class INTERSITIALDATA extends MessageComposer {
    private final String image;
    private final String url;

    public INTERSITIALDATA(String image, String url) {
        this.image = image;
        this.url = url;
    }

    @Override
    public void compose(NettyResponse response) {
        // The client reads the body as "<picture URL><TAB><click URL>" (items of the whole message), so no string
        // terminator; anything of one character or less means "no ad".
        if (this.image != null) {
            response.write(this.image + "\t" + (this.url != null ? this.url : ""));
        } else {
            response.writeInt(0);
        }
    }

    @Override
    public short getHeader() {
        return 258;
    }
}