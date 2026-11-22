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
        String[] data = content.split(Character.toString((char) 13));

        String saleCode = data[3];

        CatalogueItem item = CatalogueManager.getInstance().getCatalogueItem(saleCode);

        if (item == null) {
            return;
        }

        // The web client's purchase dialog can ask for several (a 7th line after the gift flag "0"); older clients send
        // none, and gifts, furni with purchase data (trophies, pets, wallpaper) and film are always one.
        int amount = 1;
        if (data.length > 6 && !data[5].equals("1") && StringUtils.isNumeric(data[6]) && data[6].length() <= 4) {
            int max = Math.max(1, GameConfiguration.getInstance().getInteger("catalogue.purchase.amount.max"));
            amount = Math.max(1, Math.min(max, Integer.parseInt(data[6])));
            if (amount > 1 && !CatalogueManager.canBuySeveral(item, data[4])) {
                amount = 1;
            }
        }

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
            player.send(new NO_CREDITS());
            return;
        }

        if (data[5].equals("1")) { // It's a gift!
            PlayerDetails receivingUserDetails = PlayerManager.getInstance().getPlayerData(PlayerDao.getId(data[6]));

            //if (!data[6].toLowerCase().equals(player.getDetails().getName().toLowerCase())) {
            if (receivingUserDetails == null) {
                player.send(new NO_USER_FOUND(data[6]));
                return;
            }
            //}

            String presentNote = "";
            String extraData = data[4];

            try {
                presentNote = data[7];
            } catch (Exception ignored) {
                presentNote = "";
            }

            if (presentNote.isEmpty()) {
                presentNote = " ";
            }
            
            extraData = extraData.replace(Item.PRESENT_DELIMETER, "");
            presentNote = presentNote.replace(Item.PRESENT_DELIMETER, "");

            if (item.getDefinition() != null && item.getDefinition().getSprite().equals("poster")) {
                extraData = String.valueOf(item.getItemSpecialId());
            }

            Item present = ItemManager.getInstance().createGift(receivingUserDetails, player.getDetails(), item.getSaleCode(), StringUtil.filterInput(presentNote, false), extraData);//new Item();
            /*present.setOwnerId(receivingUserDetails.getId());
            present.setDefinitionId(ItemManager.getInstance().getDefinitionBySprite("present_gen" + ThreadLocalRandom.current().nextInt(1, 7)).getId());
            present.setCustomData(saleCode +
                    Item.PRESENT_DELIMETER + player.getDetails().getName() +
                    Item.PRESENT_DELIMETER + StringUtil.filterInput(presentNote, true) +
                    Item.PRESENT_DELIMETER + extraData +
                    Item.PRESENT_DELIMETER + DateUtil.getCurrentTimeSeconds());

            ItemDao.newItem(present);*/

            Player receiver = PlayerManager.getInstance().getPlayerById(receivingUserDetails.getId());

            if (receiver != null) {
                receiver.getInventory().addItem(present);
                receiver.getInventory().getView("last");
                //receiver.send(new ITEM_DELIVERED());
            }

            player.send(new ALERT(TextsManager.getInstance().getValue("successfully_purchase_gift_for").replace("%user%", receivingUserDetails.getName())));
            //player.send(new DELIVER_PRESENT(present));
        } else {
            String extraData = null;

            if (!item.isPackage()) {
                extraData = data[4];
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

        boolean isGift = data[5].equals("1");
        Integer receiverId = null;

        if (isGift) {
            int id = PlayerDao.getId(data[6]);
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
