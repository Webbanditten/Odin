package org.alexdev.kepler.messages.incoming.rooms.user;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.player.Player;

import org.alexdev.kepler.messages.outgoing.rooms.user.PETSTAT;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;


public class GETPETSTAT implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) {
        if(player.getRoomUser().getRoom() == null) return;

        String data[] = reader.readString().split(Character.toString((char)4));
        int id = Integer.parseInt(data[0]);

        Entity entity = player.getRoomUser().getRoom().getEntityManager().getByInstanceId(id);

        if(entity == null) return;

        if(entity.getType() != EntityType.PET) return;

        Pet pet = (Pet)entity;

        player.send(new PETSTAT(id, pet));
    }

}
