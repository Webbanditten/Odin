package org.alexdev.kepler.game.room.managers;

import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.RoomManager;
import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.messages.outgoing.navigator.CANTCONNECT;
import org.alexdev.kepler.messages.outgoing.navigator.CANTCONNECT.ConnectError;
import org.alexdev.kepler.messages.outgoing.navigator.CANTCONNECT.QueueError;
import org.alexdev.kepler.messages.outgoing.rooms.ROOM_QUEUE_DATA;
import org.alexdev.kepler.messages.outgoing.rooms.ROOM_QUEUE_DATA.QueueSet;
import org.alexdev.kepler.messages.outgoing.rooms.user.REMOVE_SPECTATORS;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What happens when a room is full: its queue and its spectator seats (rooms.queue_max, rooms.spectators_max; set in
 * Housekeeping > Rooms).
 * <ul>
 *   <li>A player who asks for a full room waits in its queue (the client keeps its room loader window open with
 *   ROOM_QUEUE_DATA: "Der er 3 Habboer foran dig i køen."). Habbo Club members wait in front of everyone else. When a
 *   visitor leaves, the first in line gets in.</li>
 *   <li>With spectator seats, the window also offers "Vil du bare kigge på?": "Skift kø" (ROOM_QUEUE_CHANGE) moves the
 *   player to the spectator queue and back. A spectator gets the room without an avatar (YOUARESPECTATOR: the client
 *   shows it on its TV screen with "tilskuere 3/10"), doesn't count as a visitor and can't act in the room. With no
 *   room queue, a full room seats people as spectators straight away.</li>
 *   <li>Optional (setting room.spectator.turn.minutes, 0 = off by default): while people wait for a seat, a spectator
 *   who has watched that long makes room for the next (REMOVE_SPECTATORS: "Beklager! Nu er det en andens tur til at se
 *   eventet.").</li>
 *   <li>Without a queue or a free place: "Rummet er fyldt." (or "Køen til dette rum er fyldt op" when the queue is full).</li>
 * </ul>
 * Staff (fuse_enter_full_rooms), the room photographer and a private room's owner always get in. The queue is
 * processed every 2 seconds by the GameScheduler ({@link #processAll()}).
 */
public class RoomQueueManager {
    public static final int TARGET_SPECTATE = 1; // texts queue_current_1 / queue_other_1
    public static final int TARGET_ROOM = 2; // texts queue_current_2 / queue_other_2
    private static final int SPECTATOR_QUEUE_MAX = 100;

    private final Room room;
    private final List<Player> clubQueue = new ArrayList<>();
    private final List<Player> normalQueue = new ArrayList<>();
    private final List<Player> spectatorQueue = new ArrayList<>();

    public RoomQueueManager(Room room) {
        this.room = room;
    }

    /**
     * A player asks to enter a room (ROOM_DIRECTORY for public rooms, GOTOFLAT for private ones): in they go, or into
     * the queue, onto a spectator seat, or away with "the room is full".
     */
    public static void enter(Room room, Player player) {
        leaveQueue(player); // a queue they stood in for another room (or this one: they start again at the back)

        // The queue lives on the loaded room; a room that isn't loaded has nobody in it.
        Room loaded = RoomManager.getInstance().hasRoom(room.getId()) ? RoomManager.getInstance().getRoomById(room.getId()) : null;

        if (loaded == null) {
            room.getEntityManager().enterRoom(player, null);
            return;
        }

        loaded.getQueueManager().tryEnter(player);
    }

    /** Take the player out of the queue they wait in, if any (they cancelled, went elsewhere or logged out). */
    public static void leaveQueue(Player player) {
        Room queuedRoom = player.getQueuedRoom();

        if (queuedRoom != null) {
            queuedRoom.getQueueManager().remove(player);
        }
    }

    /** Let people in from the queues of every loaded room (GameScheduler, every 2 seconds). */
    public static void processAll() {
        for (Room room : RoomManager.getInstance().getRooms()) {
            try {
                room.getQueueManager().process();
            } catch (Exception ex) {
                Log.getErrorLogger().error("Room queue processing failed for room " + room.getId() + ": ", ex);
            }
        }
    }

    private synchronized void tryEnter(Player player) {
        if (this.mayBypass(player) || (!this.isFull() && this.isNobodyAheadOf(player))) {
            this.admit(player, false);
            return;
        }

        int queueMax = this.room.getData().getQueueMax();
        int spectatorsMax = this.room.getData().getSpectatorsMax();

        if (queueMax > 0 && this.roomQueueSize() < queueMax) {
            this.leaveCurrentRoom(player);
            (player.getDetails().hasClubSubscription() ? this.clubQueue : this.normalQueue).add(player);
            player.setQueuedRoom(this.room);
            this.sendQueueData();
            return;
        }

        if (spectatorsMax > 0 && this.spectatorQueue.size() < SPECTATOR_QUEUE_MAX) {
            if (this.spectatorQueue.isEmpty() && this.spectatorCount() < spectatorsMax) {
                this.admit(player, true);
                return;
            }

            this.leaveCurrentRoom(player);
            this.spectatorQueue.add(player);
            player.setQueuedRoom(this.room);
            this.sendQueueData();
            return;
        }

        this.leaveCurrentRoom(player); // the client is on its hotel view now
        player.send(queueMax > 0 ? new CANTCONNECT(QueueError.FULL) : new CANTCONNECT(ConnectError.ROOM_FULL));
    }

    /** "Skift kø": move between the room queue and the spectator queue (to the back of the other one). */
    public synchronized void changeQueue(Player player, int target) {
        if (player.getQueuedRoom() != this.room) {
            return;
        }

        if (target == TARGET_SPECTATE && this.room.getData().getSpectatorsMax() > 0 && this.spectatorQueue.size() < SPECTATOR_QUEUE_MAX
                && (this.clubQueue.remove(player) || this.normalQueue.remove(player))) {
            this.spectatorQueue.add(player);
        } else if (target == TARGET_ROOM && this.room.getData().getQueueMax() > 0 && this.roomQueueSize() < this.room.getData().getQueueMax()
                && this.spectatorQueue.remove(player)) {
            (player.getDetails().hasClubSubscription() ? this.clubQueue : this.normalQueue).add(player);
        } else {
            return;
        }

        if (!this.processQueues()) {
            this.sendQueueData();
        }
    }

    private synchronized void remove(Player player) {
        boolean removed = this.clubQueue.remove(player) | this.normalQueue.remove(player) | this.spectatorQueue.remove(player);

        if (player.getQueuedRoom() == this.room) {
            player.setQueuedRoom(null);
        }

        if (removed) {
            this.sendQueueData();
        }
    }

    /**
     * Let in whoever can get in now, give spectators whose turn is over's seats to those waiting, and apply a changed
     * queue or seat setting.
     */
    public synchronized void process() {
        this.processQueues();
    }

    /** @return whether anyone's place changed (they all got the new queue data then) */
    private boolean processQueues() {
        List<Player> spectators = this.spectators();

        if (this.clubQueue.isEmpty() && this.normalQueue.isEmpty() && this.spectatorQueue.isEmpty() && spectators.isEmpty()) {
            return false;
        }

        boolean changed = this.applySettings(spectators);

        this.dropGone(this.clubQueue);
        this.dropGone(this.normalQueue);
        this.dropGone(this.spectatorQueue);

        while (!this.isFull() && (!this.clubQueue.isEmpty() || !this.normalQueue.isEmpty())) {
            Player next = !this.clubQueue.isEmpty() ? this.clubQueue.remove(0) : this.normalQueue.remove(0);
            this.admit(next, false);
            changed = true;
        }

        int seats = this.room.getData().getSpectatorsMax();
        long turnSeconds = GameConfiguration.getInstance().getInteger("room.spectator.turn.minutes") * 60L;

        if (!this.spectatorQueue.isEmpty() && turnSeconds > 0) {
            int freeSeats = seats - this.spectatorCount();
            long now = DateUtil.getCurrentTimeSeconds();

            for (Player spectator : this.spectators()) { // longest watching first
                if (freeSeats >= this.spectatorQueue.size() || now - spectator.getRoomUser().getSpectatingSince() < turnSeconds) {
                    break;
                }

                this.removeSpectator(spectator);
                freeSeats++;
            }
        }

        while (!this.spectatorQueue.isEmpty() && this.spectatorCount() < seats) {
            this.admit(this.spectatorQueue.remove(0), true);
            changed = true;
        }

        if (changed) {
            this.sendQueueData();
        }

        return changed;
    }

    /** A queue switched off or made shorter, fewer seats: those it no longer has room for are told. */
    private boolean applySettings(List<Player> spectators) {
        boolean changed = false;
        int queueMax = this.room.getData().getQueueMax();

        while (this.roomQueueSize() > queueMax) {
            Player last = !this.normalQueue.isEmpty() ? this.normalQueue.remove(this.normalQueue.size() - 1) : this.clubQueue.remove(this.clubQueue.size() - 1);
            this.reset(last);
            changed = true;
        }

        int seats = this.room.getData().getSpectatorsMax();

        if (seats == 0) {
            while (!this.spectatorQueue.isEmpty()) {
                this.reset(this.spectatorQueue.remove(this.spectatorQueue.size() - 1));
                changed = true;
            }
        }

        for (int i = 0; i < spectators.size() - seats; i++) {
            this.removeSpectator(spectators.get(i));
        }

        return changed;
    }

    private void reset(Player player) {
        player.setQueuedRoom(null);
        player.send(new CANTCONNECT(QueueError.RESET)); // "Køen til dette rum er blevet nulstillet, forsøg venligst igen."
    }

    private void removeSpectator(Player spectator) {
        spectator.send(new REMOVE_SPECTATORS());
        this.room.getEntityManager().leaveRoom(spectator, true);
    }

    private void dropGone(List<Player> queue) {
        queue.removeIf(player -> {
            boolean gone = player.isDisconnected() || !player.isLoggedIn() || player.getQueuedRoom() != this.room;

            if (gone && player.getQueuedRoom() == this.room) {
                player.setQueuedRoom(null);
            }

            return gone;
        });
    }

    private void admit(Player player, boolean spectator) {
        player.setQueuedRoom(null);
        this.room.getEntityManager().enterRoom(player, null, spectator);
    }

    /** Waiting for this room happens on the hotel view: they leave the room they were in, as with entering. */
    private void leaveCurrentRoom(Player player) {
        Room current = player.getRoomUser().getRoom();

        if (current != null) {
            current.getEntityManager().leaveRoom(player, false);
        }
    }

    private boolean mayBypass(Player player) {
        if (player.hasFuse(Fuse.ENTER_FULL_ROOMS)) {
            return true; // staff and the room photographer
        }

        if (!this.room.isPublicRoom() && this.room.isOwner(player.getDetails().getId())) {
            return true;
        }

        // Already in it (asking again counts them already).
        return player.getRoomUser().getRoom() != null && player.getRoomUser().getRoom().getId() == this.room.getId()
                && !player.getRoomUser().isSpectator();
    }

    private boolean isFull() {
        return this.room.getData().getTotalVisitorsNow() >= this.room.getData().getTotalVisitorsMax();
    }

    private boolean isNobodyAheadOf(Player player) {
        return this.clubQueue.isEmpty() && (player.getDetails().hasClubSubscription() || this.normalQueue.isEmpty());
    }

    private int roomQueueSize() {
        return this.clubQueue.size() + this.normalQueue.size();
    }

    /** The room's spectators, longest watching first. */
    private List<Player> spectators() {
        List<Player> spectators = new ArrayList<>();

        for (Player player : this.room.getEntityManager().getPlayers()) {
            if (player.getRoomUser().isSpectator()) {
                spectators.add(player);
            }
        }

        spectators.sort(Comparator.comparingLong(player -> player.getRoomUser().getSpectatingSince()));
        return spectators;
    }

    public int spectatorCount() {
        return this.spectators().size();
    }

    /** Everyone waiting gets their place and the length of the other queue. */
    private void sendQueueData() {
        boolean spectatorsOn = this.room.getData().getSpectatorsMax() > 0;
        boolean queueOn = this.room.getData().getQueueMax() > 0;

        for (int i = 0; i < this.clubQueue.size(); i++) {
            this.sendRoomQueueData(this.clubQueue.get(i), new QueueSet("c", TARGET_ROOM, Map.of("c", i)), spectatorsOn);
        }

        for (int i = 0; i < this.normalQueue.size(); i++) {
            this.sendRoomQueueData(this.normalQueue.get(i), new QueueSet("d", TARGET_ROOM, Map.of("d", this.clubQueue.size() + i)), spectatorsOn);
        }

        for (int i = 0; i < this.spectatorQueue.size(); i++) {
            List<QueueSet> sets = new ArrayList<>();
            sets.add(new QueueSet("s", TARGET_SPECTATE, Map.of("d", i)));

            if (queueOn) {
                Map<String, Integer> lengths = new LinkedHashMap<>();
                lengths.put("d", this.normalQueue.size());
                lengths.put("c", this.clubQueue.size());
                sets.add(new QueueSet("dc", TARGET_ROOM, lengths));
            }

            this.spectatorQueue.get(i).send(new ROOM_QUEUE_DATA(sets));
        }
    }

    private void sendRoomQueueData(Player player, QueueSet current, boolean spectatorsOn) {
        List<QueueSet> sets = new ArrayList<>();
        sets.add(current);

        if (spectatorsOn) {
            sets.add(new QueueSet("s", TARGET_SPECTATE, Map.of("d", this.spectatorQueue.size())));
        }

        player.send(new ROOM_QUEUE_DATA(sets));
    }
}
