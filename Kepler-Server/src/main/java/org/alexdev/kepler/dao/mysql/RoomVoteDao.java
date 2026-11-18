package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room votes (users_room_votes, migration update.1.47.sql). Every vote is kept: voted_at is when it was cast, expire_time
 * is when its voter may vote on the room again (voted_at + room.rating.vote.days). A room's rating is the sum of all its
 * votes (never below 0); the day's and the week's best rooms count the votes cast in the last 24 hours / 7 days.
 */
public class RoomVoteDao {
    /**
     * Save a vote.
     *
     * @param votedAt    when it was cast (seconds)
     * @param expireTime when the voter may vote on the room again (seconds)
     */
    public static void vote(int userId, int roomId, int answer, long votedAt, long expireTime) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO users_room_votes (user_id, room_id, vote, voted_at, expire_time) VALUES (?, ?, ?, ?, ?)", sqlConnection);
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, roomId);
            preparedStatement.setInt(3, answer);
            preparedStatement.setLong(4, votedAt);
            preparedStatement.setLong(5, expireTime);
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * The voters who may not vote on the room yet, with the time (seconds) they may vote again.
     *
     * @param roomId the room
     * @param now    the current time (seconds)
     */
    public static Map<Integer, Long> getActiveVoters(int roomId, long now) {
        Map<Integer, Long> voters = new ConcurrentHashMap<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT user_id, MAX(expire_time) AS until FROM users_room_votes WHERE room_id = ? AND expire_time > ? GROUP BY user_id", sqlConnection);
            preparedStatement.setInt(1, roomId);
            preparedStatement.setLong(2, now);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                voters.put(resultSet.getInt("user_id"), resultSet.getLong("until"));
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return voters;
    }

    /**
     * The room's rating: the sum of all its votes, never below 0.
     */
    public static int getRating(int roomId) {
        int rating = 0;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT COALESCE(SUM(vote), 0) AS rating FROM users_room_votes WHERE room_id = ?", sqlConnection);
            preparedStatement.setInt(1, roomId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                rating = resultSet.getInt("rating");
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return Math.max(0, rating);
    }

    /**
     * The guest rooms with the best score from the votes cast since a time, best first (a newer last vote first between
     * equal scores). Only rooms whose score is above 0.
     *
     * @param since       votes cast at or after this time (seconds) count
     * @param limit       how many rooms
     * @param recommended only rooms the navigator may recommend: open (no doorbell or password) and not a starter room
     */
    public static List<Integer> getBestRoomsSince(long since, int limit, boolean recommended) {
        List<Integer> rooms = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT v.room_id, SUM(v.vote) AS score, MAX(v.voted_at) AS last_vote " +
                    "FROM users_room_votes v INNER JOIN rooms r ON r.id = v.room_id AND r.owner_id > 0" +
                    (recommended ? " AND r.accesstype = 0 AND r.model <> 'model_s'" : "") +
                    " WHERE v.voted_at >= ? GROUP BY v.room_id HAVING score > 0 ORDER BY score DESC, last_vote DESC LIMIT ?", sqlConnection);
            preparedStatement.setLong(1, since);
            preparedStatement.setInt(2, limit);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                rooms.add(resultSet.getInt("room_id"));
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return rooms;
    }

    /**
     * The guest rooms with the best rating (all their votes), best first. Only rooms rated above 0.
     */
    public static List<Integer> getBestRoomsEver(int limit) {
        List<Integer> rooms = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT id FROM rooms WHERE owner_id > 0 AND rating > 0 ORDER BY rating DESC, id ASC LIMIT ?", sqlConnection);
            preparedStatement.setInt(1, limit);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                rooms.add(resultSet.getInt("id"));
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return rooms;
    }
}
