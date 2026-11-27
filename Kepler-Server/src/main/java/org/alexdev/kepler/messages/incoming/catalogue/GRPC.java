package org.alexdev.kepler.messages.incoming.catalogue;

import org.alexdev.kepler.dao.mysql.PurchaseLogDao;
import org.alexdev.kepler.dao.mysql.CurrencyDao;
import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.dao.mysql.PurseDao;
import org.alexdev.kepler.game.catalogue.CatalogueItem;
import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.catalogue.CataloguePage;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.ItemManager;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.game.texts.TextsManager;
import org.alexdev.kepler.messages.outgoing.catalogue.NO_CREDITS;
import org.alexdev.kepler.messages.outgoing.rooms.items.ITEM_DELIVERED;
import org.alexdev.kepler.messages.outgoing.alert.ALERT;
import org.alexdev.kepler.messages.outgoing.user.NO_USER_FOUND;
import org.alexdev.kepler.messages.outgoing.user.currencies.CREDIT_BALANCE;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.messages.outgoing.catalogue.PURCHASE_NOT_ALLOWED;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.StringUtil;
import org.alexdev.kepler.util.config.GameConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GRPC implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        String content = reader.contents();

        if (player.hasNewCatalogue() && content.indexOf((char) 13) < 0) {
            // v31 catalogue (hh_cat_new): page id, offer id, extra parameter, as gift[, receiver, message]
            reader.readInt();
            CatalogueItem item = CatalogueManager.getInstance().getCatalogueItemById(reader.readInt());
            String extraData = reader.readString();
            boolean isGift = reader.readInt() == 1;
            String receiver = isGift ? reader.readString() : null;
            String note = isGift ? reader.readString() : null;

            if (item == null || item.isHidden()) {
                return;
            }

            this.purchase(player, item, extraData, isGift, receiver, note, 1);
            return;
        }

        // Old catalogue: production / page / language / sale code / extra / gift / receiver / message, CR separated
        String[] data = content.split(Character.toString((char) 13));
        CatalogueItem item = CatalogueManager.getInstance().getCatalogueItem(data[3]);

        if (item == null) {
            return;
        }

        // The web client's purchase dialog can ask for several: the line after the gift receiver ("0", amount) or, for a
        // gift, after its message ("1", receiver, message, amount). Older clients send none and buy one. Furni with
        // purchase data of their own (trophies, pets, wallpaper), film and pet baskets are always one; a gift of several
        // is one present that holds them all.
        boolean giftOrder = data.length > 5 && data[5].equals("1");
        int amountLine = giftOrder ? 8 : 6;
        int amount = 1;
        if (data.length > amountLine && StringUtils.isNumeric(data[amountLine]) && data[amountLine].length() <= 4) {
            amount = Math.max(1, Math.min(CatalogueManager.maxPurchaseAmount(), Integer.parseInt(data[amountLine])));
            if (amount > 1 && !CatalogueManager.canBuySeveral(item, data[4])) {
                amount = 1;
            }
        }

        String extra = data.length > 4 ? data[4] : "";
        this.purchase(player, item, extra, giftOrder, giftOrder && data.length > 6 ? data[6] : null, giftOrder && data.length > 7 ? data[7] : null, amount);
    }

    private void purchase(Player player, CatalogueItem item, String requestExtraData, boolean isGift, String receiverName, String requestNote, int amount) throws Exception {
        int price = item.getPrice() * amount;

        // The item may be on several pages: it can be bought from any page the player may open.
        List<CataloguePage> itemPages = CatalogueManager.getInstance().getCataloguePages().stream().filter(p -> item.hasPage(p.getId())).collect(Collectors.toList());

        if (itemPages.isEmpty()) {
            return; // on no page (the old code threw here)
        }

        Optional<CataloguePage> page = itemPages.stream()
                .filter(p -> player.getDetails().getFuseRights().stream().anyMatch(r -> r.getFuse().equalsIgnoreCase(p.getFuse())))
                .findFirst();

        if (page.isEmpty()) {
            // Habbo Club ran out with the catalogue open, for example: say so when club would have allowed it.
            boolean clubWouldAllow = !player.getDetails().hasClubSubscription() && PlayerDao.getFusesForPlayer(player.getDetails().getId(), true).stream()
                    .anyMatch(r -> itemPages.stream().anyMatch(p -> r.getFuse().equalsIgnoreCase(p.getFuse())));
            player.send(new PURCHASE_NOT_ALLOWED(clubWouldAllow ? PURCHASE_NOT_ALLOWED.CLUB_ONLY : PURCHASE_NOT_ALLOWED.NO_REASON));
            return;
        }

        if (price > player.getDetails().getCredits()) {
            player.send(new NO_CREDITS(player.hasNewCatalogue()));
            return;
        }

        if (isGift) { // It's a gift!
            PlayerDetails receivingUserDetails = PlayerManager.getInstance().getPlayerData(PlayerDao.getId(receiverName));

            if (receivingUserDetails == null) {
                player.send(new NO_USER_FOUND(receiverName));
                return;
            }

            String presentNote = requestNote == null ? "" : requestNote;
            String extraData = requestExtraData == null ? "" : requestExtraData;

            if (presentNote.isEmpty()) {
                presentNote = " ";
            }

            extraData = extraData.replace(Item.PRESENT_DELIMETER, "");
            presentNote = presentNote.replace(Item.PRESENT_DELIMETER, "");

            if (item.getDefinition() != null && item.getDefinition().getSprite().equals("poster")) {
                extraData = String.valueOf(item.getItemSpecialId());
            }

            Item present = ItemManager.getInstance().createGift(receivingUserDetails, player.getDetails(), item.getSaleCode(), StringUtil.filterInput(presentNote, false), extraData, amount);

            Player receiver = PlayerManager.getInstance().getPlayerById(receivingUserDetails.getId());

            if (receiver != null) {
                receiver.getInventory().addItem(present);
                receiver.getInventory().getView("last");
            }

            player.send(new ALERT(TextsManager.getInstance().getValue("successfully_purchase_gift_for").replace("%user%", receivingUserDetails.getName())));
        } else {
            String extraData = null;

            if (!item.isPackage()) {
                extraData = requestExtraData;
            }

            int bought = 0;
            for (int i = 0; i < amount; i++) {
                bought += CatalogueManager.getInstance().purchase(player, item, extraData, null, DateUtil.getCurrentTimeSeconds()).size();
            }

            if (bought > 0) {
                player.getInventory().getView("new");
            }

            boolean showItemDelivered = true;

            // Don't send item delivered if they just buy film
            if (item.getDefinition() != null && item.getDefinition().getSprite().equals("film")) {
                showItemDelivered = false;
            }

            if (showItemDelivered) {
                if (!GameConfiguration.getInstance().getBoolean("disable.purchase.successful.alert")) {
                    player.send(new ITEM_DELIVERED());
                }
            }
        }

        Integer receiverId = null;

        if (isGift) {
            int id = PlayerDao.getId(receiverName);
            receiverId = id > 0 ? id : null;
        }

        CurrencyDao.decreaseCredits(player.getDetails(), price);
        // A per-item type, so the in-game purse names what was bought (external_texts.php has a
        // transaction_system_buy_<id> / _giftbuy_<id> text for every catalogue item).
        PurseDao.logCreditSpend((isGift ? "giftbuy_" : "buy_") + item.getId(), player.getDetails(), -price);
        for (int i = 0; i < amount; i++) {
            PurchaseLogDao.log(player.getDetails(), isGift ? PurchaseLogDao.GIFT : PurchaseLogDao.CATALOGUE, receiverId,
                    item.getId(), item.getDefinition() != null ? item.getDefinition().getId() : null,
                    item.getDefinition() != null && !item.isPackage() ? item.getDefinition().getName() : item.getName(),
                    item.getPrice(), page.get().getFuse());
        }
        player.send(new CREDIT_BALANCE(player.getDetails()));
    }
}
