package org.alexdev.kepler.messages.outgoing.songs;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.List;

/**
 * Editing a Trax that uses sound sets the player doesn't have (in the machine or the hand): "Du skal have følgende Traxpacks for at redigere dette Trax:" and the sets' names. The client closes the editor.
 */
public class SONG_MISSING_PACKAGES extends MessageComposer {
    private final List<Integer> soundSets;

    public SONG_MISSING_PACKAGES(List<Integer> soundSets) {
        this.soundSets = soundSets;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.soundSets.size());

        for (int soundSet : this.soundSets) {
            response.writeInt(soundSet);
        }
    }

    @Override
    public short getHeader() {
        return 324;
    }
}
