package org.alexdev.kepler.messages.incoming.rooms.items;

import org.alexdev.kepler.dao.mysql.CatalogueDao;
import org.alexdev.kepler.dao.mysql.ItemDao;
import org.alexdev.kepler.game.catalogue.CatalogueItem;
import org.alexdev.kepler.game.catalogue.CatalogueManager;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.item.base.ItemDefinition;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.catalogue.DELIVER_PRESENT;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PRESENTOPEN implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        Room room = player.getRoomUser().getRoom();

        if (room == null) {
            return;
        }

        if (!room.isOwner(player.getDetails().getId()) && !player.hasFuse(Fuse.PICK_UP_ANY_FURNI)) {
            return;
        }

        int itemId = Integer.parseInt(reader.contents());
        Item item = room.getItemManager().getById(itemId);

        if (item == null || !item.hasBehaviour(ItemBehaviour.PRESENT)) {
            return;
        }

        String[] presentData = item.getCustomData().split(Pattern.quote(Item.PRESENT_DELIMETER));

        String saleCode = presentData[0];
        String receivedFrom = presentData[1];
        String extraData = presentData[3];
        long timestamp= Long.parseLong(presentData[4]);

        //System.out.println("Present data: " + String.join(",", presentData));
        //System.out.println("Custom data: " + item.getCustomData());
        //System.out.println(receivedFrom);

        CatalogueItem catalogueItem = null;
        boolean isListOfItemDefinitions = false;
        if (StringUtils.isNumeric(saleCode)) {
            catalogueItem = CatalogueManager.getInstance().getCatalogueItems().stream().filter(shopItem -> shopItem.getId() == Integer.parseInt(saleCode)).findFirst().orElse(null);
        } else if(saleCode.startsWith("_")) {
            isListOfItemDefinitions = true;
        } else {
            catalogueItem = CatalogueManager.getInstance().getCatalogueItem(saleCode);
        }
        if(catalogueItem == null && isListOfItemDefinitions) {
            String[] itemDefinitionIds = saleCode.replace("_", "").split(",");
            // gifted items
            List<ItemDefinition> itemDefinitions = new ArrayList<>();
            for(String itemDefinitionId : itemDefinitionIds) {
                ItemDefinition itemDefinition = ItemDao.getItemDefinition(Integer.parseInt(itemDefinitionId));
                if(itemDefinition != null) {
                    catalogueItem = CatalogueDao.getCatalogueItemByItemDefinition(itemDefinition.getId());
                    if (catalogueItem != null) {
                        itemDefinitions.add(catalogueItem.getDefinition());
                        CatalogueManager.getInstance().purchase(player, catalogueItem, extraData, receivedFrom, timestamp);
                    }
                }
            }
            var giftedItem = itemDefinitions.get(0);

            player.send(new DELIVER_PRESENT(giftedItem.getSprite(), extraData, giftedItem.getColour()));
            player.getInventory().getView("new");
            room.getMapping().removeItem(item);
            item.delete();
            return;
        }
        // Don't create a new item instance, reuse if the item isn't a trophy or teleporter, etc
        if (!catalogueItem.isPackage() && !catalogueItem.getDefinition().hasBehaviour(ItemBehaviour.PRIZE_TROPHY) &&
                !catalogueItem.getDefinition().hasBehaviour(ItemBehaviour.TELEPORTER) &&
                !catalogueItem.getDefinition().hasBehaviour(ItemBehaviour.ROOMDIMMER) &&
                !catalogueItem.getDefinition().hasBehaviour(ItemBehaviour.DECORATION) &&
                !catalogueItem.getDefinition().hasBehaviour(ItemBehaviour.POST_IT) &&
                !catalogueItem.getDefinition().getSprite().equalsIgnoreCase("film")) {
            room.getMapping().removeItem(item);

            item.setDefinitionId(catalogueItem.getDefinition().getId());
            item.setCustomData(extraData);
            item.save();

            player.send(new DELIVER_PRESENT(catalogueItem.getDefinition().getSprite(), extraData, catalogueItem.getDefinition().getColour()));

            player.getInventory().addItem(item);
            player.getInventory().getView("new");
        } else {
            List<Item> itemList = CatalogueManager.getInstance().purchase(player, catalogueItem, extraData, receivedFrom, timestamp);

            if (!itemList.isEmpty()) {
                var giftedItem = itemList.get(0);

                player.send(new DELIVER_PRESENT(giftedItem.getDefinition().getSprite(), extraData, giftedItem.getDefinition().getColour()));
                player.getInventory().getView("new");
            } else {
                // itemList will be blank if this was film purchased, however, still show film when gift is opened
                if (catalogueItem.getDefinition().getSprite().equalsIgnoreCase("film")) {
                    player.send(new DELIVER_PRESENT("film", null, null));
                }
            }

            room.getMapping().removeItem(item);
            item.delete();
        }
}
}
