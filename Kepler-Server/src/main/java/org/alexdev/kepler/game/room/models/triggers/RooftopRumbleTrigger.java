package org.alexdev.kepler.game.room.models.triggers;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.enums.StatusType;
import org.alexdev.kepler.game.triggers.GenericTrigger;
import org.alexdev.kepler.messages.outgoing.rooms.items.SHOWPROGRAM;

import java.util.concurrent.TimeUnit;

public class RooftopRumbleTrigger extends GenericTrigger {
    @Override
    public void onRoomEntry(Entity entity, Room room, boolean firstEntry, Object... customArgs) {
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player)entity;

        if (room.getTaskManager().hasTask("DivingCamera")) {
            DivingDeckTrigger.PoolCamera task = (DivingDeckTrigger.PoolCamera) room.getTaskManager().getTask("DivingCamera");

            // The camera may be between players (or the task just ended): then it picks someone on its next turn.
            if (task != null && task.getPlayer() != null) {
                player.send(new SHOWPROGRAM(new String[]{"cam1", "targetcamera", String.valueOf(task.getPlayer().getRoomUser().getInstanceId())}));
                player.send(new SHOWPROGRAM(new String[]{"cam1", "setcamera", String.valueOf(task.getCameraType())}));
            }
        } else {
            room.getTaskManager().scheduleTask("DivingCamera", new DivingDeckTrigger.PoolCamera(room), 0, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onRoomLeave(Entity entity, Room room, Object... customArgs)  {
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }

        if (room.getEntityManager().getPlayers().isEmpty()) {
            return;
        }

        Player player = (Player)entity;

        DivingDeckTrigger.PoolCamera task = (DivingDeckTrigger.PoolCamera) room.getTaskManager().getTask("DivingCamera");

        // No camera task (two players leaving at once): throwing here skipped the rest of leaveRoom, leaving the
        // player as a ghost on their tile (e.g. a Wobble Squabble start tile) and the room undisposed.
        if (task != null && task.getPlayer() == player) {
            task.spectateNewPlayer();
        }
    }
}
