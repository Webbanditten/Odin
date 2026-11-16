package org.alexdev.kepler.game.games;

import org.alexdev.kepler.dao.mysql.GameDao;
import org.alexdev.kepler.game.games.enums.GameType;
import org.alexdev.kepler.game.games.history.GameHistory;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.games.player.GameTeam;
import org.alexdev.kepler.log.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GameFinishTask implements Runnable {
    private final List<GamePlayer> players;
    private final Map<Integer, GameTeam> teams;
    private final GameType gameType;
    private final GameHistory gameHistory;

    public GameFinishTask(GameType gameType, GameHistory gameHistory, List<GamePlayer> players, Map<Integer, GameTeam> teams) {
        this.gameType = gameType;
        this.gameHistory = gameHistory;
        this.players = players;
        this.teams = teams;
    }


    @Override
    public void run() {
        var sortedTeamList = new ArrayList<>(this.teams.values());
        sortedTeamList.sort(Comparator.comparingInt(GameTeam::getPoints).reversed());

        // The game itself, for highscores and challenges ("win N games", "N points in one game").
        // A team wins with the highest score on its own; a tie for first is no win.
        int topPoints = sortedTeamList.isEmpty() ? 0 : sortedTeamList.get(0).getPoints();
        boolean tieForFirst = sortedTeamList.size() > 1 && sortedTeamList.get(1).getPoints() == topPoints;
        int gameId = -1;

        if (this.gameHistory != null && !sortedTeamList.isEmpty()) {
            gameId = GameDao.addGamePlayed(this.gameType, this.gameHistory.getName(), this.gameHistory.getGameCreator(),
                    this.gameHistory.getMapId(), sortedTeamList.size(), this.players.size(),
                    tieForFirst ? -1 : this.gameHistory.getWinningTeam(), this.gameHistory.getWinningTeamScore(),
                    this.gameHistory.getExtraData());
        }

        for (GamePlayer gamePlayer : this.players) {
            GameTeam team = this.teams.get(gamePlayer.getTeamId());
            int teamScore = team != null ? team.getPoints() : -1;
            Boolean won = team != null ? (!tieForFirst && sortedTeamList.size() > 1 && team.getPoints() == topPoints) : null;

            // Add score to the high score history
            try {
                GameDao.addHighscore(gamePlayer.getPlayer().getDetails(), this.gameType, gamePlayer.getScore(),
                        gameId, gamePlayer.getTeamId(), teamScore, won);
            } catch (SQLException throwables) {
                Log.getErrorLogger().error("Failed to add " + gamePlayer.getScore() + " points to " + gamePlayer.getPlayer().getDetails().getName());
            }
            GameDao.increasePoints(gamePlayer.getPlayer().getDetails(), this.gameType, gamePlayer.getScore());
        }
    }
}
