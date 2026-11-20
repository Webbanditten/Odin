package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Habbo Club membership history (table users_club_periods, tools/migrations/update.1.41.sql): one row per unbroken
 * membership period. Months as member, the gold badge and the monthly gifts count the time inside these periods, so a
 * break in the subscription doesn't count and doesn't reset anything either.
 */
public class ClubPeriodDao {
    /**
     * The membership time up to now, in seconds.
     */
    public static long getMemberSeconds(int userId, long now) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT COALESCE(SUM(GREATEST(0, LEAST(ends_at, ?) - started_at)), 0) AS member_seconds FROM users_club_periods WHERE user_id = ? AND started_at < ?", sqlConnection);
            preparedStatement.setLong(1, now);
            preparedStatement.setInt(2, userId);
            preparedStatement.setLong(3, now);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getLong("member_seconds");
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return 0;
    }

    /**
     * Make the history match a running subscription that ends at {@code expiration}: the open period (one that hasn't
     * ended yet) gets that end, or a new period starts now when the last one has ended (a new subscription after a break,
     * or club granted outside the game).
     */
    /** Ends a running membership period now (Housekeeping's Habbo Club admin ending a membership). */
    public static void endOpenPeriod(int userId, long now) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users_club_periods SET ends_at = ? WHERE user_id = ? AND ends_at > ?", sqlConnection);
            preparedStatement.setLong(1, now);
            preparedStatement.setInt(2, userId);
            preparedStatement.setLong(3, now);
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static void syncOpenPeriod(int userId, long now, long expiration) {
        if (expiration <= now) {
            return;
        }

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT id, ends_at FROM users_club_periods WHERE user_id = ? AND ends_at > ? ORDER BY ends_at DESC LIMIT 1", sqlConnection);
            preparedStatement.setInt(1, userId);
            preparedStatement.setLong(2, now);
            resultSet = preparedStatement.executeQuery();

            int openId = 0;
            long openEnd = 0;

            if (resultSet.next()) {
                openId = resultSet.getInt("id");
                openEnd = resultSet.getLong("ends_at");
            }

            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            resultSet = null;

            if (openId > 0) {
                if (openEnd == expiration) {
                    return;
                }

                preparedStatement = Storage.getStorage().prepare("UPDATE users_club_periods SET ends_at = ? WHERE id = ?", sqlConnection);
                preparedStatement.setLong(1, expiration);
                preparedStatement.setInt(2, openId);
            } else {
                preparedStatement = Storage.getStorage().prepare("INSERT INTO users_club_periods (user_id, started_at, ends_at) VALUES (?, ?, ?)", sqlConnection);
                preparedStatement.setInt(1, userId);
                preparedStatement.setLong(2, now);
                preparedStatement.setLong(3, expiration);
            }

            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
}
