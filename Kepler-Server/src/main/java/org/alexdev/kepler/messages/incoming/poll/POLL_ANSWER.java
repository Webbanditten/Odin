package org.alexdev.kepler.messages.incoming.poll;

import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.sql.SQLException;

public class POLL_ANSWER implements MessageEvent {

    @Override
    public void handle(Player player, NettyRequest reader) throws SQLException {
        if (!player.isLoggedIn()) {
            return;
        }
        Integer id = reader.readInt();
        System.out.println("POLL ANSWER - POLL ID: " + id);
        Integer questionId = reader.readInt();
        System.out.println("POLL ANSWER - QUESTION ID: " + questionId);


        if(questionId == 2 || questionId == 1) {
            Integer amountSelected = reader.readInt();
            System.out.println("POLL ANSWER - AMOUNT SELECTED: " + amountSelected);

            for(Integer i = 0; i < amountSelected; i++) {
                Integer selectedIndex = reader.readInt();
                System.out.println("POLL ANSWER - SELECTED: " + selectedIndex);
            }

        }

        if(questionId == 3) {
            String text = reader.readString();
            System.out.println("POLL ANSWER - TEXT: " + text);
        }

    }
}
