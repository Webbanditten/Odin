package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.moderation.cfh.CallForHelp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CallForHelpDao {

    /**
     * Log a new call for help to the database.
     *
     * @param cfh the call for help to persist
     * @param callerUsername the username of the caller
     */
    public static void logCall(CallForHelp cfh, String callerUsername) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "INSERT INTO calls_for_help (caller_id, caller_username, message, room_id, room_name, category) VALUES (?, ?, ?, ?, ?, ?)",
                sqlConnection
            );
            preparedStatement.setInt(1, cfh.getCaller());
            preparedStatement.setString(2, callerUsername);
            preparedStatement.setString(3, cfh.getMessage());
            preparedStatement.setInt(4, cfh.getRoom() != null ? cfh.getRoom().getId() : 0);
            preparedStatement.setString(5, cfh.getRoom() != null ? cfh.getRoom().getData().getName() : "");
            preparedStatement.setInt(6, cfh.getCategory());
            preparedStatement.execute();

            // Get the generated ID and set it on the CFH
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs != null && rs.next()) {
                cfh.setDatabaseId(rs.getInt(1));
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update a call for help when it gets picked up by a moderator.
     *
     * @param cfh the call for help that was picked up
     */
    public static void logPickUp(CallForHelp cfh) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "UPDATE calls_for_help SET picked_up_by = ?, picked_up_username = ?, picked_up_at = NOW() WHERE id = ?",
                sqlConnection
            );
            preparedStatement.setInt(1, cfh.getPickedUpById());
            preparedStatement.setString(2, cfh.getPickedUpBy());
            preparedStatement.setInt(3, cfh.getDatabaseId());
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update the category of a call for help in the database.
     *
     * @param cfh the call for help with the updated category
     */
    public static void updateCategory(CallForHelp cfh) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "UPDATE calls_for_help SET category = ? WHERE id = ?",
                sqlConnection
            );
            preparedStatement.setInt(1, cfh.getCategory());
            preparedStatement.setInt(2, cfh.getDatabaseId());
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Close a call for help with a reason.
     *
     * @param cfh the call for help to close
     * @param reason the reason for closing (replied, cancelled, expired)
     * @param replyMessage the reply message sent to the caller (null if not applicable)
     */
    public static void closeCall(CallForHelp cfh, String reason, String replyMessage) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                "UPDATE calls_for_help SET closed_at = NOW(), closed_reason = ?, reply_message = ? WHERE id = ?",
                sqlConnection
            );
            preparedStatement.setString(1, reason);
            preparedStatement.setString(2, replyMessage);
            preparedStatement.setInt(3, cfh.getDatabaseId());
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
}
