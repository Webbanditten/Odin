package org.alexdev.kepler.messages.incoming.songs;

import org.alexdev.kepler.messages.outgoing.songs.SOUND_PACKAGES;
import org.alexdev.kepler.messages.outgoing.songs.SONG_NEW;
import org.alexdev.kepler.messages.outgoing.songs.SONG_LIST_FULL;
import org.alexdev.kepler.messages.outgoing.songs.SONG_LOCKED;
import org.alexdev.kepler.dao.mysql.SongMachineDao;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.songs.SONG_LIST;
import org.alexdev.kepler.messages.outgoing.songs.SONG_PLAYLIST;
import org.alexdev.kepler.messages.outgoing.songs.SONG_UPDATE;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.util.StringUtil;

public class SAVE_SONG_EDIT implements MessageEvent {
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

        String title = StringUtil.filterInput(reader.readString(), true);
        String data = StringUtil.filterInput(reader.readString(), true);

        // A burned Trax plays from its discs, so it can't change: under another name it's saved as a new copy.
        if (song.isBurnt()) {
            if (title.equals(song.getTitle())) {
                player.send(new SONG_LOCKED());
                return;
            }

            if (SAVE_SONG.isMachineFull(room.getItemManager().getSoundMachine().getId())) {
                player.send(new SONG_LIST_FULL(SAVE_SONG.songsMax()));
                return;
            }

            int copyId = SongMachineDao.addSong(player.getDetails().getId(), room.getItemManager().getSoundMachine().getId(),
                    title, SAVE_SONG.calculateSongLength(data), data);

            player.send(new SOUND_PACKAGES(SongMachineDao.getTracks(room.getItemManager().getSoundMachine().getId())));
            player.send(new SONG_NEW(copyId, title));
            player.send(new SONG_LIST(SongMachineDao.getSongList(room.getItemManager().getSoundMachine().getId())));
            return;
        }

        SongMachineDao.saveSong(songId, title, SAVE_SONG.calculateSongLength(data), data);

        player.send(new SONG_UPDATE());
        player.send(new SONG_LIST(SongMachineDao.getSongList(room.getItemManager().getSoundMachine().getId())));
        room.send(new SONG_PLAYLIST(SongMachineDao.getSongPlaylist(room.getItemManager().getSoundMachine().getId())));
    }
}
