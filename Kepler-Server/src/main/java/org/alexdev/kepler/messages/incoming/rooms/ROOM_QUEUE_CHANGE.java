package org.alexdev.kepler.messages.incoming.rooms;

import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

/**
 * "Skift kø" in the room queue window: the target of the other queue (1 = watch as a spectator, 2 = the room).
 */
public class ROOM_QUEUE_CHANGE implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        Room queuedRoom = player.getQueuedRoom();

        if (queuedRoom == null) {
            return;
        }

        queuedRoom.getQueueManager().changeQueue(player, reader.readInt());
    }
}
