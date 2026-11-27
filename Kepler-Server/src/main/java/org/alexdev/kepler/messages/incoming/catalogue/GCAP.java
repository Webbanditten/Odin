package org.alexdev.kepler.messages.incoming.catalogue;

import org.alexdev.kepler.game.catalogue.CatalogueItem;
import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.catalogue.CataloguePage;
import org.alexdev.kepler.game.catalogue.RareManager;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.catalogue.CATALOGUE_PAGE;
import org.alexdev.kepler.messages.outgoing.catalogue.CATALOGUE_PAGE_V31;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class GCAP implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        // The v31 catalogue sends the page id; the old one "production/<name index>/<language>".
        if (player.hasNewCatalogue() && !reader.contents().contains("/")) {
            CataloguePage page = CatalogueManager.getInstance().getCataloguePageById(reader.readInt());

            if (page != null && CatalogueManager.getInstance().canOpen(player, page)) {
                player.send(new CATALOGUE_PAGE_V31(page, CatalogueManager.getInstance().getCataloguePageItems(page.getId())));
            }

            return;
        }

        String pageName = reader.contents().split("/")[1];

        CataloguePage cataloguePage = CatalogueManager.getInstance().getCataloguePage(pageName);

        if (cataloguePage == null || !cataloguePage.isNavigatable()) {
            return;
        }

        boolean hasFuse = player.getDetails().getFuseRights().stream().anyMatch(r -> r.getFuse().equalsIgnoreCase(cataloguePage.getFuse()));
        if (hasFuse) {
            List<CatalogueItem> catalogueItemList = CatalogueManager.getInstance().getCataloguePageItems(cataloguePage.getId());

            player.send(new CATALOGUE_PAGE(
                    cataloguePage,
                    catalogueItemList));
        }
    }
}
