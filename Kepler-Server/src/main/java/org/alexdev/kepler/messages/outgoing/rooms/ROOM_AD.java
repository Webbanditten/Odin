package org.alexdev.kepler.messages.outgoing.rooms;

        import org.alexdev.kepler.messages.types.MessageComposer;
        import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class ROOM_AD extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {
        response.writeInt(0);
        //response.writeDelimeter("/v14/c_images/ads/toilet.gif", (char) 9);
        //response.writeDelimeter("http://google.com/", (char) 13);
    }

    @Override
    public short getHeader() {
        return 208; // "CP"
    }
}
