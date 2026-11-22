package org.alexdev.kepler.game.inventory;

import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.item.interactors.InteractionType;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stacks in the hand, as in Habbo Origins: identical furni share one of the hand's nine places, up to
 * {@code hand.stack.size} (99) per stack; more of them start another stack. Off (one item per place, as before)
 * when the setting is 1 or less.
 *
 * <p>Identical = same definition and same custom data. A toggle's or a dice's custom data is only its state, so it
 * doesn't split a stack. Furni with data of their own elsewhere are never stacked: teleporters (their link), room
 * dimmers (presets), sound machines, jukeboxes and discs (songs), pet baskets (the pet), presents, photos and
 * post-it pads (the pad counts its notes in the custom data).
 *
 * <p>The hand message sends a stack as its first (newest) item, with the other items' ids in the entry's first field,
 * "SS,id,id,...", which the 2006 client never read ("SI" before). The web client's hand (hh_room_utils_stack) draws
 * the count and places or trades one item at a time from the stack.
 */
public final class HandStacks {
    private static final Set<ItemBehaviour> NEVER = EnumSet.of(
            ItemBehaviour.TELEPORTER, ItemBehaviour.ROOMDIMMER, ItemBehaviour.SOUND_MACHINE, ItemBehaviour.JUKEBOX,
            ItemBehaviour.SONG_DISK, ItemBehaviour.PRESENT, ItemBehaviour.PHOTO, ItemBehaviour.POST_IT);

    private static final Set<ItemBehaviour> STATE_ONLY = EnumSet.of(
            ItemBehaviour.CUSTOM_DATA_ON_OFF, ItemBehaviour.CUSTOM_DATA_NUMERIC_ON_OFF, ItemBehaviour.CUSTOM_DATA_TRUE_FALSE,
            ItemBehaviour.CUSTOM_DATA_NUMERIC_STATE, ItemBehaviour.DICE, ItemBehaviour.GATE);

    private HandStacks() { }

    /** Items per stack; 1 = no stacks. */
    public static int size() {
        return Math.max(1, GameConfiguration.getInstance().getInteger("hand.stack.size"));
    }

    public static boolean enabled() {
        return size() > 1;
    }

    /**
     * Group the hand's items (newest first) into stacks. A stack takes the place of its newest item; a full stack's
     * next item starts a new stack where that item is.
     */
    public static List<List<Item>> group(List<Item> items, int size) {
        List<List<Item>> stacks = new ArrayList<>();
        Map<String, List<Item>> open = new HashMap<>();

        for (Item item : items) {
            String key = size > 1 ? key(item) : null;
            List<Item> stack = key == null ? null : open.get(key);

            if (stack == null || stack.size() >= size) {
                stack = new ArrayList<>();
                stacks.add(stack);

                if (key != null) {
                    open.put(key, stack);
                }
            }

            stack.add(item);
        }

        return stacks;
    }

    /** What makes items the same, or null for furni that are never stacked. */
    static String key(Item item) {
        for (ItemBehaviour behaviour : NEVER) {
            if (item.hasBehaviour(behaviour)) {
                return null;
            }
        }

        if (item.getDefinition().getInteractionType() == InteractionType.PET_NEST) {
            return null;
        }

        boolean stateOnly = STATE_ONLY.stream().anyMatch(item::hasBehaviour);
        String data = stateOnly || item.getCustomData() == null ? "" : item.getCustomData();
        return item.getDefinition().getId() + "\u0000" + data;
    }
}
