package org.alexdev.kepler.messages.outgoing.rooms;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.List;
import java.util.Map;

/**
 * The room queue window (hh_room handle_roomqueuedata, header 259): one or two queue sets. The first is the queue the
 * player stands in, the second the other one they can switch to with "Skift kø" (ROOM_QUEUE_CHANGE). Each set has a
 * name (text "queue_set.&lt;name&gt;.info", e.g. "Der er %d% Habboer foran dig i køen."), a target (1 = watch the room
 * as a spectator, 2 = the room itself; title "queue_current_&lt;target&gt;" / "queue_other_&lt;target&gt;") and the queue
 * lengths that replace %id% in that text.
 */
public class ROOM_QUEUE_DATA extends MessageComposer {
    public record QueueSet(String name, int target, Map<String, Integer> queues) { }

    private final List<QueueSet> sets;

    public ROOM_QUEUE_DATA(List<QueueSet> sets) {
        this.sets = sets;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.sets.size());

        for (QueueSet set : this.sets) {
            response.writeString(set.name());
            response.writeInt(set.target());
            response.writeInt(set.queues().size());

            for (var queue : set.queues().entrySet()) {
                response.writeString(queue.getKey());
                response.writeInt(queue.getValue());
            }
        }
    }

    @Override
    public short getHeader() {
        return 259;
    }
}
