package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.moderation.actions.ModeratorBanUserAction;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.messages.outgoing.user.MODERATOR_ALERT;

public class RemoteBanCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        // Every name in the list (this used to ban the list's last name once per entry).
        for (String username : commandArgs.Users) {
            if (username == null) {
                continue;
            }

            PlayerDetails details = PlayerDao.getDetails(username.toLowerCase());

            if (details == null) {
                continue;
            }

            new ModeratorBanUserAction().doAction(null, details.getName(), commandArgs.BanLength, commandArgs.BanMachine, commandArgs.BanIp, commandArgs.Message, commandArgs.ExtraInfo, commandArgs.UserId);
        }

    }
}
