package org.alexdev.kepler.messages.incoming.songs;

import java.util.stream.Collectors;
import java.util.List;
import org.alexdev.kepler.messages.outgoing.songs.SONG_MISSING_PACKAGES;
import org.alexdev.kepler.dao.mysql.SongMachineDao;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.songs.USER_SOUND_PACKAGES;
import org.alexdev.kepler.messages.outgoing.songs.SONG_INFO;
import org.alexdev.kepler.messages.outgoing.songs.SOUND_PACKAGES;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

public class EDIT_SONG implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        if (player.getRoomUser().getRoom() == null) {
            return;
        }

        Room room = player.getRoomUser().getRoom();

        if (!room.isOwner(player.getDetails().getId()) && !player.hasFuse(Fuse.ANY_ROOM_CONTROLLER)) {
            return;
        }

        if (room.getItemManager().getSoundMachine() == null) {
            return;
        }

        // We don't want a user to get kicked when making cool beats
        player.getRoomUser().getTimerManager().resetRoomTimer();

        int songId = reader.readInt();
        var song = SongMachineDao.getSong(songId);

        if (song == null) {
            return;
        }

        if (song.getUserId() != player.getDetails().getId()) {
            return;
        }

        var tracks = SongMachineDao.getTracks(room.getItemManager().getSoundMachine().getId());
        var handSets = player.getInventory().getSoundsets();

        // Every sound set the song uses must be in the machine or in the hand (from where it can be inserted).
        List<Integer> missing = SAVE_SONG.soundSetsUsed(song.getData()).stream()
                .filter(set -> !tracks.containsValue(set) && !handSets.contains(set))
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            player.send(new SONG_MISSING_PACKAGES(missing));
            return;
        }

        player.send(new SONG_INFO(song));
        player.send(new SOUND_PACKAGES(tracks));
        player.send(new USER_SOUND_PACKAGES(handSets));
    }
}
