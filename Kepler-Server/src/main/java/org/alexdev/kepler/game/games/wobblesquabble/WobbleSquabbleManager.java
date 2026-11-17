package org.alexdev.kepler.game.games.wobblesquabble;


import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.util.config.GameConfiguration;

public class WobbleSquabbleManager {
    private static WobbleSquabbleManager instance;

    public static int WS_GAME_TICKET_COST = 1;
    public static int WS_BALANCE_POINTS = 35;
    public static int WS_HIT_POINTS = 13;
    public static int WS_HIT_BALANCE_POINTS = 10;
    public static int WS_GAME_TIMEOUT_SECS = 30;

    /** The game type in games_played / highscores (not a GameType: those are the lobby games). */
    public static final String HIGHSCORE_GAME_TYPE = "wobblesquabble";

    /**
     * Whether finished games go into the highscores (setting wobblesquabble.increase.points).
     */
    public static boolean isRecordingPoints() {
        return GameConfiguration.getInstance().getBoolean("wobblesquabble.increase.points");
    }

    /**
     * Highscore points for each hit that lands on the opponent (setting wobblesquabble.points.per.hit).
     */
    public static int getPointsPerHit() {
        return GameConfiguration.getInstance().getInteger("wobblesquabble.points.per.hit");
    }

    /**
     * Highscore points for knocking the opponent into the water (setting wobblesquabble.points.win).
     */
    public static int getPointsForWin() {
        return GameConfiguration.getInstance().getInteger("wobblesquabble.points.win");
    }

    /**
     * Returns true or false if the user is in a game of wobble squabble.
     *
     * @param player the player to check
     * @return true, if they are
     */
    public boolean isPlaying(Player player) {
        Room room = player.getRoomUser().getRoom();

        if (room == null) {
            return false;
        }

        if (!room.getTaskManager().hasTask(this.getName())) {
            return false;
        }

        WobbleSquabbleGame wsGame = (WobbleSquabbleGame) room.getTaskManager().getTask(this.getName());

        WobbleSquabblePlayer wsPlayer = wsGame.getPlayerById(player.getDetails().getId());
        return wsPlayer != null;
    }

    /**
     * Gets the wobble squabble player instance
     *
     * @param player the player to get for
     * @return the ws player instance, if found
     */
    public WobbleSquabblePlayer getPlayer(Player player) {
        Room room = player.getRoomUser().getRoom();

        if (room == null) {
            return null;
        }

        if (!room.getTaskManager().hasTask(this.getName())) {
            return null;
        }

        WobbleSquabbleGame wsGame = (WobbleSquabbleGame) room.getTaskManager().getTask(this.getName());
        WobbleSquabblePlayer wsPlayer = wsGame.getPlayerById(player.getDetails().getId());

        return wsPlayer;
    }

    /**
     * Get the static instance of the wobble squabble manager.
     *
     * @return the static instance
     */
    public static WobbleSquabbleManager getInstance() {
        if (instance == null) {
            instance = new WobbleSquabbleManager();
        }

        return instance;
    }

    /**
     * Gets the name of the wobble squabble game task.
     *
     * @return the game task
     */
    public String getName() {
        return "WobbleGameTask";
    }
}
