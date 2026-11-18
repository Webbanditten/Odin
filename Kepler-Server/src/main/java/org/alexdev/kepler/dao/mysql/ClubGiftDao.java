package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.util.DateUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClubGiftDao {
    /**
     * The gift progress (update.1.41.sql): left = gifts received so far (the position in the gift order, the welcome gift
     * is 0), right = membership seconds already paid out as gifts.
     */
    public static Pair<Integer, Long> getGiftProgress(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT club_gifts_claimed, club_gift_member_seconds FROM users WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return Pair.of(resultSet.getInt("club_gifts_claimed"), resultSet.getLong("club_gift_member_seconds"));
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return null;
    }

    /**
     * Claim the next gift: moves the progress on from exactly {@code claimed}, so the same gift can't be handed out
     * twice. False when the progress was already moved on.
     */
    public static boolean claimGift(int userId, int claimed, long paidOutSeconds) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET club_gifts_claimed = club_gifts_claimed + 1, club_gift_member_seconds = club_gift_member_seconds + ? WHERE id = ? AND club_gifts_claimed = ?", sqlConnection);
            preparedStatement.setLong(1, paidOutSeconds);
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(3, claimed);
            return preparedStatement.executeUpdate() > 0;
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return false;
    }

    public static Pair<Long, String> getLastGift(int userId) {
        Pair<Long, String> giftData = null;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM users_club_gifts WHERE user_id = ? ORDER BY date_received DESC LIMIT 1", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                giftData = Pair.of(resultSet.getLong("date_received"), resultSet.getString("sprite"));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return giftData;
    }

    public static void addGift(int userId, String sprite) throws SQLException {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO users_club_gifts (user_id, date_received, sprite) VALUES (?, ?, ?)", sqlConnection);
            preparedStatement.setInt(1, userId);
            preparedStatement.setLong(2, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setString(3, sprite);
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
            throw e;
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static void clearGiftHistory(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("DELETE FROM users_club_gifts WHERE user_id = ?", sqlConnection);
            preparedStatement.setInt(1, userId);
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
}
