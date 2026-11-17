package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.purse.CreditLog;
import org.alexdev.kepler.game.purse.Voucher;
import org.alexdev.kepler.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PurseDao {

    /**
     * Redeems a voucher in one transaction, so a code can't be used twice at the same moment (the game and the
     * website, or two clients): the voucher row is locked, checked (not expired; a multi-use code once per user),
     * deleted if it's single use, and the redemption is written to vouchers_history before the lock is released.
     * The CMS's website redemption takes the same lock.
     *
     * @param voucherCode the code the player typed
     * @param userId the player redeeming it
     * @return the voucher's credits and furni (one sale code per item, repeated for several), or null if there is no
     *         such voucher for this player
     */
    public static Voucher redeemVoucher(String voucherCode, int userId) {
        Voucher voucher = null;
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            sqlConnection.setAutoCommit(false);

            preparedStatement = Storage.getStorage().prepare("SELECT voucher_code, credits, is_single_use FROM vouchers WHERE voucher_code = ? " +
                    "AND (expiry_date IS NULL OR expiry_date > NOW()) FOR UPDATE", sqlConnection);
            preparedStatement.setString(1, voucherCode);
            resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                sqlConnection.rollback();
                return null;
            }

            String code = resultSet.getString("voucher_code");
            boolean isSingleUse = resultSet.getBoolean("is_single_use");
            Voucher found = new Voucher(resultSet.getInt("credits"));
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);

            if (!isSingleUse) {
                preparedStatement = Storage.getStorage().prepare("SELECT 1 FROM vouchers_history WHERE voucher_code = ? AND user_id = ? LIMIT 1", sqlConnection);
                preparedStatement.setString(1, code);
                preparedStatement.setInt(2, userId);
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    sqlConnection.rollback();
                    return null;
                }

                Storage.closeSilently(resultSet);
                Storage.closeSilently(preparedStatement);
            }

            // One row per item; a sale code listed several times gives it several times.
            preparedStatement = Storage.getStorage().prepare("SELECT catalogue_sale_code FROM vouchers_items WHERE voucher_code = ?", sqlConnection);
            preparedStatement.setString(1, code);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                found.getItems().add(resultSet.getString("catalogue_sale_code"));
            }

            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);

            if (isSingleUse) {
                preparedStatement = Storage.getStorage().prepare("DELETE FROM vouchers WHERE voucher_code = ?", sqlConnection);
                preparedStatement.setString(1, code);
                preparedStatement.executeUpdate();
                Storage.closeSilently(preparedStatement);

                preparedStatement = Storage.getStorage().prepare("DELETE FROM vouchers_items WHERE voucher_code = ?", sqlConnection);
                preparedStatement.setString(1, code);
                preparedStatement.executeUpdate();
                Storage.closeSilently(preparedStatement);
            }

            preparedStatement = Storage.getStorage().prepare("INSERT INTO vouchers_history (voucher_code, user_id, credits_redeemed, items_redeemed) VALUES (?, ?, ?, ?)", sqlConnection);
            preparedStatement.setString(1, code);
            preparedStatement.setInt(2, userId);

            if (found.getCredits() > 0) {
                preparedStatement.setInt(3, found.getCredits());
            } else {
                preparedStatement.setNull(3, Types.INTEGER);
            }

            String items = formatItems(found.getItems());

            if (items != null) {
                preparedStatement.setString(4, items);
            } else {
                preparedStatement.setNull(4, Types.VARCHAR);
            }

            preparedStatement.executeUpdate();
            sqlConnection.commit();
            voucher = found;
        } catch (Exception e) {
            Storage.logError(e);

            try {
                if (sqlConnection != null) {
                    sqlConnection.rollback();
                }
            } catch (Exception ignored) { }
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);

            try {
                if (sqlConnection != null) {
                    sqlConnection.setAutoCommit(true);
                }
            } catch (Exception ignored) { }

            Storage.closeSilently(sqlConnection);
        }

        return voucher;
    }

    /** vouchers_history.items_redeemed: "count,sale code" per distinct sale code, joined by "|" (null without items). */
    private static String formatItems(List<String> saleCodes) {
        if (saleCodes.isEmpty()) {
            return null;
        }

        Map<String, Integer> counts = new java.util.LinkedHashMap<>();

        for (String saleCode : saleCodes) {
            counts.merge(saleCode, 1, Integer::sum);
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("|");
            }

            stringBuilder.append(entry.getValue()).append(",").append(entry.getKey());
        }

        return stringBuilder.toString();
    }

    public static void logCreditSpend(String type, PlayerDetails player, int credits) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO credit_log (user_id, timestamp, type, credits) VALUES (?, ?, ?, ?)", sqlConnection);

            preparedStatement.setInt(1, player.getId());
            preparedStatement.setLong(2, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setString(3, type);
            preparedStatement.setInt(4, credits);
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static List<CreditLog> getCreditLog(int userId) {
        List<CreditLog> creditLogs = new CopyOnWriteArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT user_id, type, timestamp, credits FROM credit_log WHERE user_id = ?", sqlConnection);
            preparedStatement.setLong(1, userId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                creditLogs.add(new CreditLog(
                        resultSet.getInt("user_id"), resultSet.getLong("timestamp"),
                        resultSet.getString("type"), resultSet.getInt("credits")));
            }
        } catch (Exception e) {
            System.out.println(e);
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return creditLogs;
    }
}
