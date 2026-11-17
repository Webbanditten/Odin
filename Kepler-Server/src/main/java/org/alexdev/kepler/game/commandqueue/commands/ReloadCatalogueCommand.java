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
            // Only clients that loaded a catalogue get the new index, in their catalogue's format; the others load it
            // when they open it (an old client refreshes with an empty request, which mustn't look like the v31 catalogue).
            if (p.getNewCatalogue() == null) {
                continue;
            }

            CatalogueManager.getInstance().sendIndex(p);
            p.send(new REFRESH_CATALOGUE());
        }
    }
}
