package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.game.ads.AdManager;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;

/** Sent by the CMS after a change in Housekeeping > Ads: players see the new ads from their next room entry. */
public class ReloadAdsCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        AdManager.reset();
    }
}
