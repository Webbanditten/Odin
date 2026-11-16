package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.player.PlayerDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Writes purchase_log (tools/migrations/update.1.37.sql): what each purchase bought, for the website's
 * popular-furni box and the housekeeping purchase history. The in-game purse still reads credit_log.
 */
public class PurchaseLogDao {
    public static final String CATALOGUE = "catalogue";
    public static final String GIFT = "gift";
    public static final String CLUB = "club";
    public static final String TICKETS = "tickets";
    public static final String SONG = "song";

    /**
     * Log a purchase. Never throws: a failed log line must not undo or block the purchase.
     *
     * @param receiverId       the gift or ticket receiver, or null
     * @param catalogueItemId  the catalogue_items id, or null
     * @param definitionId     the items_definitions id, or null
     * @param pageFuse         the catalogue page's fuse, or null outside the catalogue
     */
    public static void log(PlayerDetails buyer, String kind, Integer receiverId, Integer catalogueItemId,
                           Integer definitionId, String itemName, int credits, String pageFuse) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO purchase_log (user_id, kind, receiver_id, catalogue_item_id, definition_id, item_name, credits, page_fuse, source) VALUES (?,?,?,?,?,?,?,?,'game')", sqlConnection);
            preparedStatement.setInt(1, buyer.getId());
            preparedStatement.setString(2, kind);
            setNullable(preparedStatement, 3, receiverId);
            setNullable(preparedStatement, 4, catalogueItemId);
            setNullable(preparedStatement, 5, definitionId);
            preparedStatement.setString(6, itemName == null ? "" : (itemName.length() > 255 ? itemName.substring(0, 255) : itemName));
            preparedStatement.setInt(7, credits);
            if (pageFuse != null) preparedStatement.setString(8, pageFuse); else preparedStatement.setNull(8, Types.VARCHAR);
            preparedStatement.execute();
        } catch (SQLException e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    private static void setNullable(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value != null) {
            statement.setInt(index, value);
        } else {
            statement.setNull(index, Types.INTEGER);
        }
    }
}
