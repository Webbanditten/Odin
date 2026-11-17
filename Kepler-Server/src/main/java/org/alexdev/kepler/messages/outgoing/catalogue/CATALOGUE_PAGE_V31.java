package org.alexdev.kepler.messages.outgoing.catalogue;

import org.alexdev.kepler.game.catalogue.CatalogueItem;
import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.catalogue.CataloguePackage;
import org.alexdev.kepler.game.catalogue.CataloguePage;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.item.base.ItemDefinition;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.List;

/**
 * A catalogue page for the v31 catalogue (hh_cat_new), header 127 like the old one:
 * page id, layout, images[], texts[], then offers: id, name (the productdata key), credits, pixels, and the furni
 * in it (type s/i, class id, extra parameter, count, expiration). The class id is the definition id: furnidata.txt
 * (generated from items_definitions) maps it to the class name, colours and size.
 */
public class CATALOGUE_PAGE_V31 extends MessageComposer {
    private final CataloguePage page;
    private final List<CatalogueItem> items;

    public CATALOGUE_PAGE_V31(CataloguePage page, List<CatalogueItem> items) {
        this.page = page;
        this.items = items.stream().filter(CATALOGUE_PAGE_V31::isSendable).toList();
    }

    private static boolean isSendable(CatalogueItem item) {
        if (item.isPackage()) {
            return item.getPackages().stream().anyMatch(p -> p.getDefinition() != null);
        }

        return item.getDefinition() != null;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.page.getId());
        response.writeString(this.page.getV31Layout());

        List<String> images = this.page.getV31Images();
        response.writeInt(images.size());

        for (String image : images) {
            response.writeString(image);
        }

        List<String> texts = this.page.getV31Texts();
        response.writeInt(texts.size());

        for (String text : texts) {
            response.writeString(text);
        }

        response.writeInt(this.items.size());

        for (CatalogueItem item : this.items) {
            response.writeInt(item.getId());
            response.writeString(CatalogueManager.getInstance().getV31OfferName(item));
            response.writeInt(item.getPrice());
            response.writeInt(0); // pixels: not used here (pixels.enabled is off)

            if (item.isPackage()) {
                List<CataloguePackage> parts = item.getPackages().stream().filter(p -> p.getDefinition() != null).toList();
                response.writeInt(parts.size());

                for (CataloguePackage part : parts) {
                    writeProduct(response, part.getDefinition(), part.getSpecialSpriteId(), part.getAmount());
                }
            } else {
                response.writeInt(1);
                writeProduct(response, item.getDefinition(), item.getItemSpecialId(), 1);
            }
        }
    }

    private static void writeProduct(NettyResponse response, ItemDefinition definition, int specialSpriteId, int count) {
        response.writeString(definition.hasBehaviour(ItemBehaviour.WALL_ITEM) ? "i" : "s");
        response.writeInt(definition.getId());
        response.writeString(specialSpriteId > 0 ? String.valueOf(specialSpriteId) : "");
        response.writeInt(Math.max(1, count));
        response.writeInt(-1); // expiration: not a rental
    }

    @Override
    public short getHeader() {
        return 127; // "A"
    }
}
