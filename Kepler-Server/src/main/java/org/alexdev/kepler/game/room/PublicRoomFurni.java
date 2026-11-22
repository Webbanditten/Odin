package org.alexdev.kepler.game.room;

import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.item.interactors.InteractionType;
import org.alexdev.kepler.game.player.Player;

import java.util.EnumSet;
import java.util.Set;

/**
 * Staff with {@code fuse_public_room_furni} place ordinary furni in public rooms (for events and the like), move, turn,
 * switch and pick them up there; everyone else sees them like the room's own furniture. In public rooms they get the
 * room controller and owner flags (G_STAT), so the web client's info stand shows Move, Turn and Pick up on furni that
 * were placed (never on the room's own, PUBLIC_SPACE_OBJECT). {@code :pickall} takes them all back.
 *
 * <p>Only furni that work in a public room may be placed: floor furni, but not those that need things public rooms don't
 * have (wall items: public rooms load none, and their walls are room art; teleporters link guest rooms; rollers don't
 * run; pets live in guest rooms; songs belong to a room owner; presents and credit furni are opened by a room owner;
 * vending machines: public rooms hand out no drinks from furni; the pool, queue and game furni are public room parts).
 */
public final class PublicRoomFurni {
    private static final Set<ItemBehaviour> NOT_IN_PUBLIC_ROOMS = EnumSet.of(
            ItemBehaviour.WALL_ITEM, ItemBehaviour.POST_IT, ItemBehaviour.PHOTO, ItemBehaviour.ROOMDIMMER,
            ItemBehaviour.DECORATION, ItemBehaviour.TELEPORTER, ItemBehaviour.ROLLER, ItemBehaviour.PLACE_ROLLER_ON_TOP,
            ItemBehaviour.PRESENT, ItemBehaviour.REDEEMABLE, ItemBehaviour.SOUND_MACHINE,
            ItemBehaviour.SOUND_MACHINE_SAMPLE_SET, ItemBehaviour.JUKEBOX, ItemBehaviour.SONG_DISK,
            ItemBehaviour.PET_WATER_BOWL, ItemBehaviour.PET_FOOD, ItemBehaviour.PET_CAT_FOOD, ItemBehaviour.PET_DOG_FOOD,
            ItemBehaviour.PET_CROC_FOOD, ItemBehaviour.PET_TOY, ItemBehaviour.PUBLIC_SPACE_OBJECT,
            ItemBehaviour.PRIVATE_FURNITURE, ItemBehaviour.INVISIBLE);

    private static final Set<InteractionType> NOT_IN_PUBLIC_ROOMS_INTERACTIONS = EnumSet.of(
            InteractionType.TELEPORT, InteractionType.VENDING_MACHINE, InteractionType.PET_NEST, InteractionType.PET_FOOD,
            InteractionType.PET_WATER_BOWL, InteractionType.PET_TOY, InteractionType.POOL_ENTER, InteractionType.POOL_EXIT,
            InteractionType.POOL_BOOTH, InteractionType.POOL_LIFT, InteractionType.QUEUE_TILE,
            InteractionType.GAME_TIC_TAC_TOE, InteractionType.GAME_CHESS, InteractionType.GAME_BATTLESHIPS,
            InteractionType.GAME_POKER, InteractionType.WS_JOIN_QUEUE, InteractionType.WS_QUEUE_TILE,
            InteractionType.WS_TILE_START);

    private PublicRoomFurni() { }

    /** Whether the player places and edits furni in this room as a public room furni editor. */
    public static boolean mayEdit(Player player, Room room) {
        return room != null && room.isPublicRoom() && !room.isGameArena() && player.hasFuse(Fuse.PUBLIC_ROOM_FURNI);
    }

    /** Whether this furni may be placed in a public room. */
    public static boolean canPlace(Item item) {
        if (item.getDefinition() == null) {
            return false;
        }

        for (ItemBehaviour behaviour : NOT_IN_PUBLIC_ROOMS) {
            if (item.hasBehaviour(behaviour)) {
                return false;
            }
        }

        return !NOT_IN_PUBLIC_ROOMS_INTERACTIONS.contains(item.getDefinition().getInteractionType());
    }

    /** A furni someone placed, not part of the public room itself. */
    public static boolean isPlaced(Item item) {
        return !item.hasBehaviour(ItemBehaviour.PUBLIC_SPACE_OBJECT);
    }
}
