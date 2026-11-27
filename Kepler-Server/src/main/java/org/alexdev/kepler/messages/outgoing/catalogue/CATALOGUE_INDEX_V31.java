package org.alexdev.kepler.messages.outgoing.catalogue;

import org.alexdev.kepler.game.catalogue.CataloguePage;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The catalogue index for the v31 catalogue (hh_cat_new): a tree of nodes, each
 * visible, colour, icon, page id (-1 = folder), name, disabled, child count, children (Catalogue Handler parseIndexNode).
 * Same header as the old index (126).
 */
public class CATALOGUE_INDEX_V31 extends MessageComposer {
    private final List<CataloguePage> pages;
    private final Set<Integer> pageIds;

    /** @param pages the pages this player may see (index visible, fuse, club), in index order */
    public CATALOGUE_INDEX_V31(List<CataloguePage> pages) {
        this.pages = pages;
        this.pageIds = pages.stream().map(CataloguePage::getId).collect(Collectors.toSet());
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(0); // navigatable
        response.writeInt(0); // colour
        response.writeInt(0); // icon
        response.writeInt(-1); // page id
        response.writeString("");
        response.writeInt(0); // disabled
        this.writeChildren(response, -1);
    }

    private void writeChildren(NettyResponse response, int parentId) {
        // The pages come in order_id order; the tree goes by tree_order, and pages not placed in it (0) follow the
        // placed ones in that order (a page added by a script shows up at the end of its folder, not at the top).
        List<CataloguePage> children = this.pages.stream().filter(p -> this.parentOf(p) == parentId)
                .sorted(Comparator.comparingInt((CataloguePage p) -> p.getTreeOrder() > 0 ? 0 : 1).thenComparingInt(CataloguePage::getTreeOrder))
                .toList();
        response.writeInt(children.size());

        for (CataloguePage page : children) {
            // "navigatable" makes the client draw the node; a folder (page without a layout) has page id -1, so
            // clicking it only opens and closes it (Treeview Interface Class).
            response.writeInt(1);
            response.writeInt(page.getColour());
            response.writeInt(page.getIcon());
            response.writeInt(page.isNavigatable() ? page.getId() : -1);
            response.writeString(page.getName());
            response.writeInt(0); // disabled
            this.writeChildren(response, page.getId());
        }
    }

    /** A page whose parent the player can't see is shown at the top, so nothing gets lost. */
    private int parentOf(CataloguePage page) {
        int parentId = page.getParentId();
        return parentId > 0 && parentId != page.getId() && this.pageIds.contains(parentId) ? parentId : -1;
    }

    @Override
    public short getHeader() {
        return 126; // "A~"
    }
}
