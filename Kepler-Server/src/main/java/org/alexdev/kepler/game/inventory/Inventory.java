package org.alexdev.kepler.game.inventory;

import org.alexdev.kepler.dao.mysql.ItemDao;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.inventory.INVENTORY;
import org.alexdev.kepler.server.netty.streams.NettyResponse;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.StringUtil;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.*;

public class Inventory {
    private static final int MAX_ITEMS_PER_PAGE = 9;

    private Player player;
    private List<Item> items;

    /** The hand's pages: up to nine stacks each (with stacks off, every stack is one item). */
    private Map<Integer, List<List<Item>>> paginatedItems;
    private int stackCount;
    private boolean stacked;
    private int handStripPageIndex;

    public Inventory(Player player) {
        this.player = player;
        this.reload();
    }

    /**
     * Reload inventory.
     */
    public void reload() {
        this.handStripPageIndex = 0;
        this.items = ItemDao.getInventory(this.player.getDetails().getId());
        this.refreshPagination();

        // Prevent possible virtual item dupes
        if (this.player.getRoomUser().getRoom() != null) {
            this.items.removeIf(x -> this.player.getRoomUser().getRoom().getItems().stream().anyMatch(item -> x.getId() == item.getId()));
        }
    }

    public void delete() {
        ItemDao.deleteAllNotInRoom(this.player.getDetails().getId());
        this.items = ItemDao.getInventory(this.player.getDetails().getId());
        this.refreshPagination();
    }

    /**
     * Refreshes the pagination by making the most recently bought items appear first.
     */
    private void refreshPagination() {
        int orderId = 0;

        for (Item item : this.items) {
            if (orderId != item.getOrderId()) {
                item.setOrderId(orderId);
                item.save();
            }

            orderId++;

        }

        List<Item> tempList = new ArrayList<>();

        for (Item item : this.items) {
            // Don't show items if they are hidden
            if (item.isHidden()) {
                continue;
            }

            // Don't show items if they are currently in trade window
            if (this.player.getRoomUser().getTradePartner() != null) {
                if (this.player.getRoomUser().getTradeItems().contains(item)) {
                    continue;
                }
            }

            tempList.add(item);
        }

        this.items.sort(Comparator.comparingInt(Item::getOrderId));

        this.stacked = HandStacks.enabled();
        List<List<Item>> stacks = HandStacks.group(tempList, HandStacks.size());
        this.stackCount = stacks.size();
        this.paginatedItems = StringUtil.paginate(stacks, MAX_ITEMS_PER_PAGE);
    }

    /**
     * Get the view of the inventory.
     *
     * @param stripView the view type
     */
    public void getView(String stripView) {
        this.refreshPagination();
        this.changeView(stripView);

        Map<Integer, List<Item>> casts = this.getCasts();
        this.player.send(new INVENTORY(this, casts));
    }

    /**
     * Get the inventory casts for opening hand: the current page's stacks by their place in the hand.
     */
    private Map<Integer, List<Item>> getCasts() {
        Map<Integer, List<Item>> casts = new LinkedHashMap<>();

        if (this.paginatedItems.containsKey(this.handStripPageIndex)) {
            int stripSlotId = (this.handStripPageIndex * MAX_ITEMS_PER_PAGE);

            for (List<Item> stack : this.paginatedItems.get(this.handStripPageIndex)) {
                casts.put(stripSlotId++, stack);
            }
        }

        return casts;
    }

    /** Whether the last hand page was sent as stacks. */
    public boolean isStacked() {
        return stacked;
    }

    /** How many places (stacks) the hand has, over all pages. */
    public int getStackCount() {
        return stackCount;
    }

    /**
     * Change the inventory view over.
     *
     * @param stripView the strip view to change
     */
    private void changeView(String stripView) {
        if (stripView.equals("new")) {
            this.handStripPageIndex = 0;
        }

        if (stripView.equals("delete")) {
            this.handStripPageIndex = 0;
            player.getInventory().delete();
        }

        if (stripView.equals("next")) {
            this.handStripPageIndex++;
        }

        if (stripView.equals("prev")) {
            this.handStripPageIndex--;
        }

        if (stripView.equals("last")) {
            this.handStripPageIndex = this.paginatedItems.size() - 1;
        }

        if (stripView.equals("update")) {
            if (this.handStripPageIndex > this.paginatedItems.size() - 1) {
                this.handStripPageIndex = this.paginatedItems.size() - 1;
            }
        }

        if (!this.paginatedItems.containsKey(this.handStripPageIndex)) {
            this.handStripPageIndex = 0;
        }
    }

    /**
     * Serialise item in hand.
     *
     * @param response the response to write the item to
     * @param item the item to use the data for the packet
     * @param stripSlotId the slot in the hand
     */
    public static void serialise(NettyResponse response, Item item, int stripSlotId) {
        serialise(response, List.of(item), stripSlotId, false);
    }

    /**
     * Serialise a hand stack: its first item, with the other items' ids in the first field ("SS,id,id,...")
     * when the hand is stacked. The 2006 client doesn't read that field ("SI").
     */
    public static void serialise(NettyResponse response, List<Item> stack, int stripSlotId, boolean stacked) {
        Item item = stack.get(0);

        if (stacked) {
            StringBuilder ids = new StringBuilder("SS");

            for (int i = 1; i < stack.size(); i++) {
                ids.append(',').append(stack.get(i).getId());
            }

            response.writeDelimeter(ids.toString(), (char) 30);
        } else {
            response.writeDelimeter("SI", (char) 30);
        }

        response.writeDelimeter(item.getId(), (char) 30);
        response.writeDelimeter(stripSlotId, (char) 30);

        if (item.hasBehaviour(ItemBehaviour.WALL_ITEM)) {
            response.writeDelimeter("I", (char) 30);
        } else {
            response.writeDelimeter("S", (char) 30);
        }

        response.writeDelimeter(item.getId(), (char) 30);
        response.writeDelimeter(item.getDefinition().getSprite(), (char) 30);

        if (item.hasBehaviour(ItemBehaviour.WALL_ITEM)) {
            response.writeDelimeter(item.getCustomData(), (char) 30);
            response.writeDelimeter("0", (char) 30);
        } else {
            response.writeDelimeter(item.getDefinition().getLength(), (char) 30);
            response.writeDelimeter(item.getDefinition().getWidth(), (char) 30);
            response.writeDelimeter(item.getCustomData(), (char) 30);
            response.writeDelimeter(item.getDefinition().getColour(), (char) 30);

            // Recyclable: past the quarantine (a stack only when all of its items are, as the client may hand in any)
            long today = DateUtil.getCurrentTimeSeconds();
            long quarantineSeconds = GameConfiguration.getInstance().getInteger("recycler.item.quarantine.seconds");
            boolean recyclable = item.getDefinition().isRecyclable()
                    && stack.stream().allMatch(x -> today - x.getOwnedSince() > quarantineSeconds);
            response.writeDelimeter(recyclable ? "1" : "0", (char) 30);

            response.writeDelimeter(item.getDefinition().getSprite(), (char) 30);
        }

        response.write("/");
    }

    /**
     * Get inventory item by id.
     *
     * @param itemId the id used to get the inventory item
     * @return the inventory item
     */
    public Item getItem(int itemId) {
        for (Item item : this.items) {
            if (item.getId() == itemId) {
                return item;
            }
        }

        return null;
    }

    /**
     * Get all soundset track IDs within the inventory.
     *
     * @return the list of soundsets
     */
    public List<Integer> getSoundsets() {
        List<Integer> handSoundsets = new ArrayList<>();

        for (Item item : player.getInventory().getItems()) {
            if (item.isHidden()) {
                continue;
            }
            
            if (item.hasBehaviour(ItemBehaviour.SOUND_MACHINE_SAMPLE_SET)) {
                handSoundsets.add(Integer.parseInt(item.getDefinition().getSprite().split("_")[2]));
            }
        }

        return handSoundsets;
    }

    /**
     * Add the item to the start of items list.
     *
     * @param item the item
     */
    public void addItem(Item item) {
        this.items.remove(item);
        this.items.add(0, item);
    }

    /**
     * Get the list of inventory items.
     *
     * @return the list of items
     */
    public List<Item> getItems() {
        return items;
    }
}
