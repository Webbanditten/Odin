package org.alexdev.kepler.messages.incoming.rooms;

import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.messages.outgoing.navigator.CANTCONNECT;
import org.alexdev.kepler.messages.outgoing.navigator.CANTCONNECT.QueueError;
import org.alexdev.kepler.messages.outgoing.rooms.OPEN_CONNECTION;
import org.alexdev.kepler.messages.outgoing.rooms.ROOM_SPECTATORCOUNT;
import org.alexdev.kepler.messages.outgoing.rooms.user.YOUARESPECTATOR;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

public class ROOM_GETSPECTATORCOUNT implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        player.send(new ROOM_SPECTATORCOUNT());
     }
}
