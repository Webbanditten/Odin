package org.alexdev.kepler.messages.incoming.games;

import org.alexdev.kepler.util.config.GameConfiguration;
import org.alexdev.kepler.messages.outgoing.games.WATCH_FAILED;
import org.alexdev.kepler.messages.outgoing.games.INSTANCE_NOT_AVAILABLE;
import org.alexdev.kepler.game.games.Game;
import org.alexdev.kepler.game.games.GameManager;
import org.alexdev.kepler.game.games.enums.GameState;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.triggers.GameLobbyTrigger;
import org.alexdev.kepler.messages.outgoing.games.GAMEINSTANCE;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

public class WATCHGAME implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        if (player.getRoomUser().getRoom() == null) {
            return;
        }

        Room room = player.getRoomUser().getRoom();

        if (!(room.getModel().getRoomTrigger() instanceof GameLobbyTrigger)) {
            return;
        }

        if (player.getRoomUser().getGamePlayer() != null) {
            return;
        }

        int gameId = reader.readInt();

        Game game = GameManager.getInstance().getGameById(gameId);

        if (game == null) {
            player.send(new INSTANCE_NOT_AVAILABLE(gameId));
            return;
        }

        int spectatorsMax = GameConfiguration.getInstance().getInteger("games.spectators.max");

        if (spectatorsMax > 0 && game.getSpectators().size() >= spectatorsMax) {
            player.send(new WATCH_FAILED(gameId, 0)); // "Alle tilskuerpladserne er optagede!"
            return;
        }

        GamePlayer gamePlayer = new GamePlayer(player);
        gamePlayer.setGameId(gameId);
        gamePlayer.setSpectator(true);

        player.getRoomUser().setGamePlayer(gamePlayer);

        game.getSpectators().add(gamePlayer);

        if (game.getGameState() == GameState.STARTED) {
            game.sendSpectatorToArena(gamePlayer);
        } else {
            game.send(new GAMEINSTANCE(game));
            game.sendObservers(new GAMEINSTANCE(game));
        }
    }
}
