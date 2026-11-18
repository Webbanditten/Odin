package org.alexdev.kepler.messages.outgoing.catalogue;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

/**
 * A catalogue purchase was refused (hh_cat_code handle_purchasenotallowed). Code 1 = it needs Habbo Club ("For at kunne
 * købe denne ting, skal du være medlem af Habbo Club!"); 0 = no message.
 */
public class PURCHASE_NOT_ALLOWED extends MessageComposer {
    public static final int NO_REASON = 0;
    public static final int CLUB_ONLY = 1;

    private final int code;

    public PURCHASE_NOT_ALLOWED(int code) {
        this.code = code;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.code);
    }

    @Override
    public short getHeader() {
        return 296;
    }
}
