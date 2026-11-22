package org.alexdev.kepler.messages.outgoing.inventory;

import org.alexdev.kepler.game.inventory.Inventory;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.List;
import java.util.Map;

public class INVENTORY extends MessageComposer {
    private final Inventory inventory;
    private final Map<Integer, List<Item>> casts;

    public INVENTORY(Inventory inventory, Map<Integer, List<Item>> casts) {
        this.inventory = inventory;
        this.casts = casts;
    }

    @Override
    public void compose(NettyResponse response) {
        for (var kvp : this.casts.entrySet()) {
            Inventory.serialise(response, kvp.getValue(), kvp.getKey(), this.inventory.isStacked());
        }

        // The client pages with this: the number of places in the hand (stacks), or of items without stacks.
        response.write((char) 13);
        response.write(this.inventory.isStacked() ? this.inventory.getStackCount() : this.inventory.getItems().size());
    }

    @Override
    public short getHeader() {
        return 140; // "BL"
    }
}
