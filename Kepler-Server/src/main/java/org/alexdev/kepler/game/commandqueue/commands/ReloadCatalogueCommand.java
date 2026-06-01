package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.item.ItemManager;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.messages.outgoing.catalogue.CATALOGUE_PAGES;
import org.alexdev.kepler.messages.outgoing.catalogue.REFRESH_CATALOGUE;

public class ReloadCatalogueCommand implements Command {
    public void executeCommand(CommandTemplate commandArgs) {
        ItemManager.reset();
        CatalogueManager.reset();

        for (Player p : PlayerManager.getInstance().getPlayers()) {
            p.send(new CATALOGUE_PAGES(
                    CatalogueManager.getInstance().getPagesForRank(p.getDetails().getFuseRights(), p.getDetails().hasClubSubscription())
            ));
            p.send(new REFRESH_CATALOGUE());
        }
    }
}
