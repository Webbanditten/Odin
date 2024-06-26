package org.alexdev.kepler.game.moderation.actions;

import org.alexdev.kepler.dao.mysql.ModerationDao;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.moderation.AuditLogType;
import org.alexdev.kepler.game.moderation.ModerationAction;
import org.alexdev.kepler.game.moderation.ModerationActionType;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.user.MODERATOR_ALERT;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.util.List;

public class ModeratorRoomAlertAction implements ModerationAction {
    @Override
    public void performAction(Player player, Room room, String alertMessage, String notes, NettyRequest reader) {
        if (!player.hasFuse(Fuse.ROOM_ALERT)) {
            return;
        }

        List<Player> players = player.getRoomUser().getRoom().getEntityManager().getPlayers();

        for (Player target : players) {
            target.send(new MODERATOR_ALERT(alertMessage));
        }

        ModerationDao.addLog(AuditLogType.ROOM_ALERT, player.getDetails().getId(), 0, alertMessage, notes, 0);
    }
}
