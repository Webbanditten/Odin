package org.alexdev.kepler.messages.incoming.rooms;

import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.rooms.user.SPECTATOR_AMOUNT;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

/**
 * The spectator counter on the TV screen ("tilskuere 3/10"); the client asks every 15 seconds.
 */
public class GET_SPECTATOR_AMOUNT implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        Room room = player.getRoomUser().getRoom();

        if (room == null || !player.getRoomUser().isSpectator()) {
            player.send(new SPECTATOR_AMOUNT(-1, 0));
            return;
        }

        player.send(new SPECTATOR_AMOUNT(room.getQueueManager().spectatorCount(), room.getData().getSpectatorsMax()));
    }
}
