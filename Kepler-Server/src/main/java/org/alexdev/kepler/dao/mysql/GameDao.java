package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.games.GameSpawn;
import org.alexdev.kepler.game.games.enums.GameType;
import org.alexdev.kepler.game.games.battleball.BattleBallMap;
import org.alexdev.kepler.game.games.player.GameRank;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.room.models.RoomModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GameDao {
    public static List<GameRank> getRanks() {
        List<GameRank> ranks = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_ranks", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ranks.add(new GameRank(resultSet.getInt("id"), resultSet.getString("type"),
                        resultSet.getString("title"), resultSet.getInt("min_points"),
                        resultSet.getInt("max_points")));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return ranks;
    }

    public static List<RoomModel> getGameMaps() {
        List<RoomModel> maps = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_maps", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String modelName = "bb" + "_arena_" + resultSet.getInt("map_id");

                if (!resultSet.getString("game_type").equals("battleball")) {
                    modelName = "ss_arena_" + resultSet.getInt("map_id");
                }

                maps.add(new RoomModel(modelName, modelName, Integer.MAX_VALUE, Integer.MAX_VALUE, Double.MAX_VALUE, 0, resultSet.getString("heightmap"), null));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }


        return maps;
    }

    public static List<BattleBallMap> getBattleballTileMaps() {
        List<BattleBallMap> maps = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_maps WHERE game_type = 'battleball'", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                maps.add(new BattleBallMap(resultSet.getInt("map_id"), GameType.BATTLEBALL, resultSet.getString("tile_map")));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }


        return maps;
    }

    public static List<GameSpawn> getGameSpawns() {
        List<GameSpawn> spawns = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM games_player_spawns", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                spawns.add(new GameSpawn(resultSet.getInt("team_id"),  resultSet.getInt("map_id"), resultSet.getString("type"),
                        resultSet.getInt("x"), resultSet.getInt("y"), resultSet.getInt("rotation")));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }


        return spawns;
    }
    /**
     * Record a finished game (highscores and challenges read it through highscores.game_id).
     *
     * @return the new games_played id, or -1 if it couldn't be stored
     */
    public static int addGamePlayed(GameType type, String name, String creator, int mapId, int teamCount, int playerCount,
                                    int winningTeam, int winningTeamScore, String extraData) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet keys = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = sqlConnection.prepareStatement("INSERT INTO games_played (gametype, name, creator, map_id, team_count, player_count, winning_team, winning_team_score, extra_data) VALUES (?,?,?,?,?,?,?,?,?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, type.name().toLowerCase());
            preparedStatement.setString(2, name == null ? "" : name);
            preparedStatement.setString(3, creator == null ? "" : creator);
            preparedStatement.setInt(4, mapId);
            preparedStatement.setInt(5, teamCount);
            preparedStatement.setInt(6, playerCount);
            preparedStatement.setInt(7, winningTeam);
            preparedStatement.setInt(8, winningTeamScore);
            preparedStatement.setString(9, extraData == null ? "" : extraData);
            preparedStatement.executeUpdate();

            keys = preparedStatement.getGeneratedKeys();
            return keys != null && keys.next() ? keys.getInt(1) : -1;
        } catch (SQLException e) {
            Storage.logError(e);
            return -1;
        } finally {
            Storage.closeSilently(keys);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static void addHighscore(PlayerDetails details, GameType type, int amount) throws SQLException {
        addHighscore(details, type, amount, -1, -1, -1, null);
    }

    /**
     * Add a player's score for one game. gameId, teamId and teamScore below 0 and a null won are stored as NULL.
     */
    public static void addHighscore(PlayerDetails details, GameType type, int amount, int gameId, int teamId, int teamScore, Boolean won) throws SQLException {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO highscores (uid, gametype, score, game_id, team_id, team_score, won) VALUES (?,?,?,?,?,?,?)", sqlConnection);
            preparedStatement.setInt(1, details.getId());
            preparedStatement.setString(2, type.name().toLowerCase());
            preparedStatement.setInt(3, amount);
            if (gameId > 0) preparedStatement.setInt(4, gameId); else preparedStatement.setNull(4, java.sql.Types.INTEGER);
            if (teamId >= 0) preparedStatement.setInt(5, teamId); else preparedStatement.setNull(5, java.sql.Types.INTEGER);
            if (teamScore >= 0) preparedStatement.setInt(6, teamScore); else preparedStatement.setNull(6, java.sql.Types.INTEGER);
            if (won != null) preparedStatement.setBoolean(7, won); else preparedStatement.setNull(7, java.sql.Types.TINYINT);
            preparedStatement.execute();

        } catch (SQLException e) {
            Storage.logError(e);
            throw e;
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
    /**
     * Atomically increase tickets.
     *
     * @param details the player details
     */
    public static void increasePoints(PlayerDetails details, GameType type, int amount) {
        String column = type.name().toLowerCase() + "_points";

        Connection conn = null;
        PreparedStatement updateQuery = null;
        PreparedStatement fetchQuery = null;
        ResultSet row = null;

        try {
            conn = Storage.getStorage().getConnection();

            // We disable autocommit to make sure the following queries share the same atomic transaction
            conn.setAutoCommit(false);

            // Increase credits
            updateQuery = Storage.getStorage().prepare("UPDATE users SET " + column + " = " + column + " + ? WHERE id = ?", conn);
            updateQuery.setInt(1, amount);
            updateQuery.setInt(2, details.getId());
            updateQuery.execute();

            // Fetch increased amount
            fetchQuery = Storage.getStorage().prepare("SELECT " + column + " FROM users WHERE id = ?", conn);
            fetchQuery.setInt(1, details.getId());
            row = fetchQuery.executeQuery();

            // Commit these queries
            conn.commit();

            // Set amount
            if (row != null && row.next()) {
                int updatedAmount = row.getInt(column);

                if (type == GameType.BATTLEBALL) {
                    details.setBattleballPoints(updatedAmount);
                }

                if (type == GameType.SNOWSTORM) {
                    details.setSnowStormPoints(updatedAmount);
                }
            }

        } catch (Exception e) {
            try {
                // Rollback these queries
                conn.rollback();
            } catch(SQLException re) {
                Storage.logError(re);
            }

            Storage.logError(e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ce) {
                Storage.logError(ce);
            }

            Storage.closeSilently(row);
            Storage.closeSilently(updateQuery);
            Storage.closeSilently(fetchQuery);
            Storage.closeSilently(conn);
        }
    }
}
