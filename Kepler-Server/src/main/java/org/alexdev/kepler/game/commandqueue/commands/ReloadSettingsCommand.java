package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.game.club.ClubSubscription;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.util.config.GameConfiguration;
import org.alexdev.kepler.util.config.writer.GameConfigWriter;

/**
 * A setting was changed in housekeeping: reload the game settings (as ":reload settings" in game), then refresh the
 * Habbo Club badges of everyone online, so a new club.gold.months applies straight away.
 */
public class ReloadSettingsCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        GameConfiguration.reset(new GameConfigWriter());

        for (Player player : PlayerManager.getInstance().getPlayers()) {
            ClubSubscription.refreshBadge(player);
        }
    }
}
