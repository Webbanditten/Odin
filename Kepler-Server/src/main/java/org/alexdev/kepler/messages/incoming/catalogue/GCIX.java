package org.alexdev.kepler.messages.incoming.catalogue;

import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.catalogue.CataloguePage;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.catalogue.CATALOGUE_PAGES;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.util.ArrayList;
import java.util.List;

public class GCIX implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        // The old catalogue (hh_cat_code) sends "production/<language>" when it opens, and nothing when it refreshes
        // after REFRESH_CATALOGUE; the v31 one (hh_cat_new) always sends nothing. So an empty request is the v31
        // catalogue unless this client already showed it runs the old one.
        String contents = reader.contents();

        if (contents != null && !contents.isEmpty()) {
            player.setNewCatalogue(false);
        } else if (!Boolean.FALSE.equals(player.getNewCatalogue())) {
            player.setNewCatalogue(true);
        }

        CatalogueManager.getInstance().sendIndex(player);
    }
}
