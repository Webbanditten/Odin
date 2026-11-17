package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerManager;

/**
 * The CMS put furni in a player's hand (e.g. a voucher redeemed on the website): if they are online, reload their hand
 * from the database and show it, so the items appear without Odin creating them a second time.
 */
public class ReloadInventoryCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        Player player = PlayerManager.getInstance().getPlayerById(commandArgs.UserId);

        if (player == null) {
            return;
        }

        player.getInventory().reload();
        player.getInventory().getView("new");
    }
}
