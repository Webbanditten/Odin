package org.alexdev.kepler.game.catalogue;

import org.alexdev.kepler.dao.mysql.*;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.ItemManager;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.item.base.ItemDefinition;
import org.alexdev.kepler.game.item.interactors.InteractionType;
import org.alexdev.kepler.util.config.GameConfiguration;
import org.alexdev.kepler.game.pets.PetManager;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.catalogue.CATALOGUE_INDEX_V31;
import org.alexdev.kepler.messages.outgoing.catalogue.CATALOGUE_PAGES;
import org.alexdev.kepler.messages.outgoing.user.currencies.FILM;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CatalogueManager {
    private static CatalogueManager instance;

    private List<CataloguePage> cataloguePageList;
    private List<CatalogueItem> catalogueItemList;
    private List<CataloguePackage> cataloguePackageList;
    private java.util.Set<String> libraryImages;

    public CatalogueManager() {
        this.cataloguePageList = CatalogueDao.getPages();
        this.cataloguePackageList = CatalogueDao.getPackages();
        this.catalogueItemList = CatalogueDao.getItems();
        this.libraryImages = CatalogueDao.getLibraryImages();
        this.loadPackages();
    }

    /** Most of one catalogue item a purchase (or a gift) may hold: {@code catalogue.purchase.amount.max}, at least 1. */
    public static int maxPurchaseAmount() {
        return Math.max(1, GameConfiguration.getInstance().getInteger("catalogue.purchase.amount.max"));
    }

    /**
     * Whether one purchase may buy several of this item: never furni whose purchase carries data (a trophy's
     * inscription, a pet's name, a wallpaper's pattern), film, decoration, trophies or pet baskets.
     */
    public static boolean canBuySeveral(CatalogueItem item, String extraData) {
        if (extraData != null && !extraData.isEmpty() && !extraData.equals("-")) {
            return false;
        }
        if (item.isPackage()) {
            return true;
        }
        ItemDefinition def = item.getDefinition();
        if (def == null || def.getSprite().equals("film")) {
            return false;
        }
        return !def.hasBehaviour(ItemBehaviour.DECORATION) && !def.hasBehaviour(ItemBehaviour.PRIZE_TROPHY)
                && def.getInteractionType() != InteractionType.PET_NEST;
    }

    /** Page images that come from the image library (see catalogue_library_images). */
    public java.util.Set<String> getLibraryImages() {
        return libraryImages;
    }

    public List<Item> purchase(Player player, CatalogueItem item, String extraData, String overrideName, long timestamp) throws SQLException {
        List<Item> itemsBought = new ArrayList<>();

        if (!item.isPackage()) {
            Item newItem = purchase(player, item.getDefinition(), extraData, item.getItemSpecialId(), overrideName, timestamp);

            if (newItem != null) {
                itemsBought.add(newItem);
            }
        } else {
            for (CataloguePackage cataloguePackage : item.getPackages()) {
                for (int i = 0; i < cataloguePackage.getAmount(); i++) {
                    Item newItem = purchase(player, cataloguePackage.getDefinition(), null, cataloguePackage.getSpecialSpriteId(), overrideName, timestamp);
                    itemsBought.add(newItem);
                }
            }
        }

        return itemsBought;
    }

    private Item purchase(Player player, ItemDefinition def, String extraData, int specialSpriteId, String overrideName, long timestamp) throws SQLException {
        // If the item is film, just give the user film
        if (def.getSprite().equals("film")) {
            CurrencyDao.increaseFilm(player.getDetails(), 5);
            player.send(new FILM(player.getDetails()));
            return null;
        }

        String customData = "";

        if (extraData != null) {
            if (def.hasBehaviour(ItemBehaviour.DECORATION)) {
                customData = extraData;
            } else {
                if (specialSpriteId > 0) {
                    customData = String.valueOf(specialSpriteId);
                }
            }

            if (def.hasBehaviour(ItemBehaviour.POST_IT)) {
                customData = "20";
            }

            if (def.hasBehaviour(ItemBehaviour.PRIZE_TROPHY)) {
                customData += (overrideName != null ? overrideName : player.getDetails().getName());
                customData += (char)9;

                customData += DateUtil.getShortDate(timestamp);
                customData += (char)9;

                customData += StringUtil.filterInput(extraData, true);
            }

            if (def.hasBehaviour(ItemBehaviour.ROOMDIMMER)) {
                customData = Item.DEFAULT_ROOMDIMMER_CUSTOM_DATA;
            }
        }

        Item item = new Item();
        item.setOwnerId(player.getDetails().getId());
        item.setDefinitionId(def.getId());

        if (def.getInteractionType() == InteractionType.PET_NEST) {
            if (extraData != null) {
                item.setDefinitionId(ItemManager.getInstance().getDefinitionBySprite("nest").getId());
            }
        }

        item.setCustomData(customData);
        item.setOwnedSince(DateUtil.getCurrentTimeSeconds());
        ItemDao.newItem(item);
        player.getInventory().addItem(item);

        // If the item is a camera, give them 2 free film.
        if (def.getSprite().equals("camera")) {
            CurrencyDao.increaseFilm(player.getDetails(), 2);
            player.send(new FILM(player.getDetails()));
        }

        if (def.hasBehaviour(ItemBehaviour.TELEPORTER)) {
            Item linkedTeleporterItem = new Item();
            linkedTeleporterItem.setOwnerId(player.getDetails().getId());
            linkedTeleporterItem.setDefinitionId(def.getId());
            linkedTeleporterItem.setCustomData(customData);
            linkedTeleporterItem.setOwnedSince(DateUtil.getCurrentTimeSeconds());
            ItemDao.newItem(linkedTeleporterItem);
            player.getInventory().addItem(linkedTeleporterItem);

            linkedTeleporterItem.setTeleporterId(item.getId());
            item.setTeleporterId(linkedTeleporterItem.getId());

            TeleporterDao.addPair(linkedTeleporterItem.getId(), item.getId());
            TeleporterDao.addPair(item.getId(), linkedTeleporterItem.getId());
        }

        if (def.getInteractionType() == InteractionType.PET_NEST) {
            if (extraData != null) {
                String[] petData = extraData.split(Character.toString((char) 2));
                String name = StringUtil.filterInput(petData[0], true);
                String type = def.getSprite().replace("pets", "");
                int race = Integer.valueOf(petData[1]);
                String color = StringUtil.filterInput(petData[2], true);

                if (PetManager.getInstance().isValidName(player.getDetails().getName(), name)) {
                    PetDao.createPet(item.getId(), name, type, race, color);
                }
            }
        }

        return item;
    }

    /**
     * Load catalogue packages for all catalogue items.
     */
    private void loadPackages() {
        for (CatalogueItem catalogueItem : this.catalogueItemList) {
            if (!catalogueItem.isPackage()) {
                continue;
            }

            for (CataloguePackage cataloguePackage : this.cataloguePackageList) {
                if (catalogueItem.getSaleCode().equals(cataloguePackage.getSaleCode())) {
                    catalogueItem.getPackages().add(cataloguePackage);
                }
            }
        }
    }

    /**
     * Get a page by the page index
     *
     * @param pageIndex the index of the page to get for
     * @return the catalogue page
     */
    public CataloguePage getCataloguePage(String pageIndex) {
        for (CataloguePage cataloguePage : this.cataloguePageList) {
            if (cataloguePage.getNameIndex().equals(pageIndex)) {
                return cataloguePage;
            }
        }

        return null;
    }

    /**
     * Get an item by it's sale code.
     *
     * @param saleCode the item sale code identifier
     * @return the item, if successful
     */
    public CatalogueItem getCatalogueItem(String saleCode) {
        for (CatalogueItem catalogueItem : this.catalogueItemList) {
            if (catalogueItem.isHidden()) {
                continue;
            }

            if (catalogueItem.getSaleCode().equals(saleCode)) {
                return catalogueItem;
            }
        }

        return null;
    }

    /**
     * The catalogue item a voucher gives for a sale code: a visible one if there is one, else a hidden one, so vouchers
     * can hand out furni that isn't for sale (rares, prizes).
     *
     * @param saleCode the sale code from vouchers_items
     * @return the item, or null if no catalogue item has that sale code
     */
    public CatalogueItem getVoucherItem(String saleCode) {
        CatalogueItem hidden = null;

        for (CatalogueItem catalogueItem : this.catalogueItemList) {
            if (!catalogueItem.getSaleCode().equals(saleCode)) {
                continue;
            }

            if (!catalogueItem.isHidden()) {
                return catalogueItem;
            }

            if (hidden == null) {
                hidden = catalogueItem;
            }
        }

        return hidden;
    }

    /**
     * Get a list of items for the catalogue page.
     *
     * @param pageId the id of the page to get the items for
     * @return the list of items
     */
    public List<CatalogueItem> getCataloguePageItems(int pageId) {
        List<CatalogueItem> items = new ArrayList<>();

        for (CatalogueItem catalogueItem : this.catalogueItemList) {
            if (catalogueItem.isHidden()) {
                continue;
            }

            if (catalogueItem.hasPage(pageId)) {
                items.add(catalogueItem);
            }
        }

        return items;
    }

    /**
     * Get catalogue page list.
     *
     * @return the list of catalogue pages
     */
    public List<CataloguePage> getCataloguePages() {
        return this.cataloguePageList;
    }

    /**
     * Get catalogue page list for a certain rank
     *
     * @return the list of catalogue pages
     */
    public List<CataloguePage> getPagesForRank(List<Fuseright> fuses, boolean hasClub) {
        List<CataloguePage> cataloguePagesForRank = new ArrayList<>();

        for (CataloguePage page : this.cataloguePageList) {
            if (!page.isIndexVisible()) {
                continue;
            }

            if (page.isClubOnly() && !hasClub) {
                continue;
            }

            boolean hasFuse = fuses.stream().anyMatch(r -> r.getFuse().equalsIgnoreCase(page.getFuse()));
            if (hasFuse) {
                cataloguePagesForRank.add(page);
            }
        }

        return cataloguePagesForRank;
    }

    /**
     * Send the catalogue index the player's client understands: the v31 tree (hh_cat_new) or the old list.
     */
    public void sendIndex(Player player) {
        List<CataloguePage> pages = this.getPagesForRank(player.getDetails().getFuseRights(), player.getDetails().hasClubSubscription());

        if (player.hasNewCatalogue()) {
            player.send(new CATALOGUE_INDEX_V31(pages));
        } else {
            // The old catalogue has a flat index: v31 folders (pages without a layout) aren't pages there.
            player.send(new CATALOGUE_PAGES(pages.stream().filter(CataloguePage::isNavigatable).toList()));
        }
    }

    /**
     * Whether the player may open the page: its fuse, and Habbo Club for club pages.
     */
    public boolean canOpen(Player player, CataloguePage page) {
        if (page.isClubOnly() && !player.getDetails().hasClubSubscription()) {
            return false;
        }

        return player.getDetails().getFuseRights().stream().anyMatch(r -> r.getFuse().equalsIgnoreCase(page.getFuse()));
    }

    public CataloguePage getCataloguePageById(int id) {
        return this.cataloguePageList.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public CatalogueItem getCatalogueItemById(int id) {
        return this.catalogueItemList.stream().filter(i -> i.getId() == id).findFirst().orElse(null);
    }

    /**
     * Get catalogue items list.
     *
     * @return the list of items packages
     */
    public List<CatalogueItem> getCatalogueItems() {
        return catalogueItemList;
    }

    /**
     * Get the {@link CatalogueManager} instance
     *
     * @return the catalogue manager instance
     */
    public static CatalogueManager getInstance() {
        if (instance == null) {
            instance = new CatalogueManager();
        }

        return instance;
    }

    /**
     * Resets the catalogue manager singleton.
     */
    public static void reset() {
        instance = null;
        CatalogueManager.getInstance();
    }
}
