package org.alexdev.kepler.messages.outgoing.moderation;

import org.alexdev.kepler.game.moderation.cfh.CallForHelp;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class CALL_FOR_HELP extends MessageComposer {
    private CallForHelp cfh;

    public CALL_FOR_HELP(CallForHelp cfh){
        this.cfh = cfh;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeString(this.cfh.getCryId());
        response.writeInt(this.cfh.getCategory());
        response.writeString(this.cfh.getFormattedRequestTime());
        response.writeString(this.cfh.getCallerName().isEmpty() ? String.valueOf(this.cfh.getCaller()) : this.cfh.getCallerName());
        response.writeString(this.cfh.getMessage());

        if (this.cfh.getRoom() == null) {
            // Not in a room (a reported console message on the hotel view): no room to go to.
            response.writeString("0");
            response.writeString("");
            response.writeInt(3); // neither public (0), private (1) nor game (2)
            response.writeString("");
            return;
        }

        response.writeString(String.valueOf(this.cfh.getRoom().getId()));
        response.writeString(this.cfh.getRoom().getData().getName());

        {
            if (this.cfh.getRoom().isPublicRoom()) {
                response.writeInt(0);
                response.writeString(this.cfh.getRoom().getData().getCcts());
                response.writeInt(this.cfh.getRoom().getId() + RoomManager.PUBLIC_ROOM_OFFSET);
                response.writeInt(0);
            } else {
                response.writeInt(1);
                response.writeString(this.cfh.getRoom().getData().getName());
                response.writeInt(this.cfh.getRoom().getId());
                response.writeString(this.cfh.getRoom().getData().getOwnerName());
            }
        }
    }

    @Override
    public short getHeader() {
        return 148; // "BT"
    }
}
