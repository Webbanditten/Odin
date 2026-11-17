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
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.StringUtil;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.Optional;

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

            this.purchase(player, item, extraData, isGift, receiver, note);
            return;
        }

        // Old catalogue: production / page / language / sale code / extra / gift / receiver / message, CR separated
        String[] data = content.split(Character.toString((char) 13));
        CatalogueItem item = CatalogueManager.getInstance().getCatalogueItem(data[3]);

        if (item == null) {
            return;
        }

        boolean isGift = data[5].equals("1");
        this.purchase(player, item, data[4], isGift, isGift ? data[6] : null, isGift && data.length > 7 ? data[7] : null);
    }

    private void purchase(Player player, CatalogueItem item, String requestExtraData, boolean isGift, String receiverName, String requestNote) throws Exception {
        int price = item.getPrice();

        Optional<CataloguePage> pageStream = CatalogueManager.getInstance().getCataloguePages().stream().filter(p -> item.hasPage(p.getId())).findFirst();

        if (!pageStream.isPresent()) {
            return;
        }

        boolean hasFuse = player.getDetails().getFuseRights().stream().anyMatch(r -> r.getFuse().equalsIgnoreCase(pageStream.get().getFuse()));
        if (!hasFuse) {
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

            Item present = ItemManager.getInstance().createGift(receivingUserDetails, player.getDetails(), item.getSaleCode(), StringUtil.filterInput(presentNote, false), extraData);

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

            if (CatalogueManager.getInstance().purchase(player, item, extraData, null, DateUtil.getCurrentTimeSeconds()).size() > 0) {
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
        PurchaseLogDao.log(player.getDetails(), isGift ? PurchaseLogDao.GIFT : PurchaseLogDao.CATALOGUE, receiverId,
                item.getId(), item.getDefinition() != null ? item.getDefinition().getId() : null,
                item.getDefinition() != null && !item.isPackage() ? item.getDefinition().getName() : item.getName(),
                price, pageStream.get().getFuse());
        player.send(new CREDIT_BALANCE(player.getDetails()));
    }
}
