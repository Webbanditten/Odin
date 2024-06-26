package org.alexdev.kepler.dao.mysql;

import com.goterl.lazysodium.interfaces.PwHash;
import org.alexdev.kepler.Kepler;
import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.fuserights.Fuseright;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.tag.Tag;
import org.alexdev.kepler.util.DateUtil;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerDao {


    /**
     * Resets the login streak for a given user
     * @param userId
     */
    public static void resetLoginStreak(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET login_streak = 0, last_streak = ? WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setInt(2, userId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Increments the login streak for a given user
     * @param userId
     */
    public static void incrementLoginStreak(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET login_streak = login_streak + 1, last_streak = ? WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setInt(2, userId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }
    /**
     * Logs the machine id for a given user
     *
     * @param userId the user id to edit
     * @param machineId the machine id
     */
    public static void logMachineId(int userId, String machineId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO users_machineid_logs (user_id, machine_id) VALUES (?, ?)", sqlConnection);
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, machineId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }


    /**
     * Gets the machine for a given user
     *
     * @param userId the user id to edit
     */
    public static String getLatestMachineId(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String ip = "-";

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT machine_id FROM users_machineid_logs WHERE user_id = ? ORDER BY created_at DESC", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                ip = resultSet.getString("machine_id");
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return ip;
    }


    /**
     * Logs the IP address for a given user
     *
     * @param userId the user id to edit
     * @param ipAddress the ip address
     */
    public static void logIpAddress(int userId, String ipAddress) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO users_ip_logs (user_id, ip_address) VALUES (?, ?)", sqlConnection);
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, ipAddress);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }


    /**
     * Gets the IP address for a given user
     *
     * @param userId the user id to edit
     */
    public static String getLatestIp(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        String ip = "-";

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT ip_address FROM users_ip_logs WHERE user_id = ? ORDER BY created_at DESC", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                ip = resultSet.getString("ip_address");
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return ip;
    }

    /**
     * Gets the tags for a user by id
     *
     * @param userId the user id
     * @return tags
     */
    public static Map<Integer, Tag> getTags(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        Map<Integer, Tag> tags = new HashMap<>();

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM tags WHERE user_id = ? LIMIT 8", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                tags.put(resultSet.getInt("id"), new Tag(
                        resultSet.getInt("id"), resultSet.getString("tag"), resultSet.getInt("user_id")));

            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return tags;
    }

    /**
     * Gets the details by user id
     *
     * @param userId the user id
     * @return the details
     */
    public static PlayerDetails getDetails(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        PlayerDetails details = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM users WHERE id = ? LIMIT 1", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                details = new PlayerDetails();
                fill(details, resultSet);
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return details;
    }

    /**
     * Gets the details by username
     *
     * @param username the username
     * @return the details
     */
    public static PlayerDetails getDetails(String username) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        PlayerDetails details = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM users WHERE username = ? LIMIT 1", sqlConnection);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                details = new PlayerDetails();
                fill(details, resultSet);
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return details;
    }

    /**
     * Login with SSO ticket.
     *
     * @param player the player
     * @param ssoTicket the sso ticket
     * @return true, if successful
     */
    public static boolean loginTicket(Player player, String ssoTicket) {
        boolean success = false;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM users WHERE sso_ticket = ? LIMIT 1", sqlConnection);
            preparedStatement.setString(1, ssoTicket);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                fill(player.getDetails(), resultSet);
                success = true;
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return success;
    }

    public static boolean login(String username, String password) {
        boolean success = false;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT id, password FROM users WHERE username = ? LIMIT 1", sqlConnection);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                byte[] hashedPassword = (resultSet.getString("password") + '\0').getBytes(StandardCharsets.UTF_8);
                byte[] pass = password.getBytes(StandardCharsets.UTF_8);

                PwHash.Native pwHash = (PwHash.Native) Kepler.getLibSodium();
                success = pwHash.cryptoPwHashStrVerify(hashedPassword, pass, pass.length);
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return success;
    }

    /**
     * Login with SSO ticket.
     *
     * @param player the player
     * @param username username
     * @param password password
     * @return true, if successful
     */
    public static boolean login(PlayerDetails player, String username, String password) {
        boolean success = false;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM users WHERE username = ? LIMIT 1", sqlConnection);
            preparedStatement.setString(1, username);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                byte[] hashedPassword = (resultSet.getString("password") + '\0').getBytes(StandardCharsets.UTF_8);
                byte[] pass = password.getBytes(StandardCharsets.UTF_8);

                PwHash.Native pwHash = (PwHash.Native) Kepler.getLibSodium();
                success = pwHash.cryptoPwHashStrVerify(hashedPassword, pass, pass.length);

                if (success) {
                    fill(player, resultSet);
                }
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return success;
    }

    /**
     * Increments the online time for the player
     *
     * @param userId ID of user
     * @param seconds Seconds to add
     */
    public static void incrementOnlineTime(int userId, long seconds) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET online_time_seconds = online_time_seconds + ? WHERE id = ?", sqlConnection);
            preparedStatement.setLong(1, seconds);
            preparedStatement.setInt(2, userId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Increments the times a player has logged in
     *
     * @param userId ID of user
     */
    public static void incrementLoginCounter(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET times_logged_in = times_logged_in + 1 WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, userId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Clear SSO ticket
     * Protects against replay attacks
     *
     * @param userId ID of user
     */
    public static void clearSSOTicket(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET sso_ticket = ? WHERE id = ? LIMIT 1", sqlConnection);
            preparedStatement.setNull(1, Types.VARCHAR);
            preparedStatement.setInt(2, userId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Gets the id.
     *
     * @param username the username
     * @return the id
     */
    public static int getId(String username) {
        int id = -1;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT id FROM users WHERE LOWER(username) = ? LIMIT 1", sqlConnection);
            preparedStatement.setString(1, username.toLowerCase());
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                id = resultSet.getInt("id");
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return id;
    }

    /**
     * Gets the fuses for the users rank.
     *
     * @param rankId the rank id
     * @return the name
     */
    public static List<Fuseright> getFusesForRank (int rankId, boolean hasClub) {
        List<Fuseright> fuses = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String habboClubQuery = (hasClub) ? " OR user_group = 'HABBO_CLUB'" : "";
        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT fuse FROM rank_rights where rank_id = ? UNION SELECT fuse FROM fuses where user_group = 'ANYONE'" + habboClubQuery, sqlConnection);
            preparedStatement.setInt(1, rankId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                fuses.add(new Fuseright(resultSet.getString("fuse").toLowerCase()));

            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return fuses;
    }

    /**
     * Gets the fuses for a specific user.
     *
     * @param userId user id of user
     * @return the name
     */
    public static List<Fuseright> getFusesForPlayer (int userId, boolean hasClub) {
        List<Fuseright> fuses = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String habboClubQuery = (hasClub) ? " OR user_group = 'HABBO_CLUB'" : "";
        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT fuse FROM rank_rights where rank_id = (SELECT rank from users where id = ?) UNION SELECT fuse FROM fuses where user_group = 'ANYONE'" + habboClubQuery, sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                fuses.add(new Fuseright(resultSet.getString("fuse")));

            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return fuses;
    }

    /**
     * Gets the name.
     *
     * @param id the id
     * @return the name
     */
    public static String getName(int id) {
        String name = null;

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT username FROM users WHERE id = ? LIMIT 1", sqlConnection);
            preparedStatement.setInt(1, id);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                name = resultSet.getString("username");
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return name;
    }

    /**
     * Register user
     */
    public static void register(String username, String password, String figure, String sex, String email, String birthday, boolean directMail){
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = Storage.getStorage().getConnection();
            stmt = Storage.getStorage().prepare("INSERT INTO users (username, password, figure, sex, pool_figure, sso_ticket, email, birthday, receive_email) VALUES (?, ?, ?, ?, '', '', ?, ?, ?)", conn);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, figure);
            stmt.setString(4, sex);
            stmt.setString(5, email);
            stmt.setString(6, birthday);
            stmt.setInt(7, directMail ? 1 : 0);
            stmt.execute();
        } catch (SQLException e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(stmt);
            Storage.closeSilently(conn);
        }
    }
    /**
     * Update Password
     *
     * @param details the details of the user
     */
    public static void savePassword(PlayerDetails details) {

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET password = ? WHERE id = ?", sqlConnection);
            preparedStatement.setString(1, details.getPassword());
            preparedStatement.setInt(2, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update last online.
     *
     * @param details the details of the user
     */
    public static void saveLastOnline(PlayerDetails details) {
        long currentTime = DateUtil.getCurrentTimeSeconds();
        details.setLastOnline(currentTime);

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET last_online = ? WHERE id = ?", sqlConnection);
            preparedStatement.setLong(1, currentTime);
            preparedStatement.setInt(2, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static void setPlayerOnline(PlayerDetails details) {
        long currentTime = DateUtil.getCurrentTimeSeconds();
        details.setLastOnline(currentTime);

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET status = 'online' WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static void setPlayerOffline(PlayerDetails details) {
        long currentTime = DateUtil.getCurrentTimeSeconds();
        details.setLastOnline(currentTime);

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET status = 'offline' WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }



    /**
     * Update sound setting.
     *
     * @param details the details of the user
     */
    public static void saveSoundSetting(PlayerDetails details) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET sound_enabled = ? WHERE id = ?", sqlConnection);
            preparedStatement.setBoolean(1, details.getSoundSetting());
            preparedStatement.setInt(2, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update details.
     *
     * @param details the player details to save
     */
    public static void saveDetails(PlayerDetails details) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET figure = ?, pool_figure = ?, sex = ? WHERE id = ?", sqlConnection);
            preparedStatement.setString(1, details.getFigure());
            preparedStatement.setString(2, details.getPoolFigure());
            preparedStatement.setString(3, Character.toString(details.getSex()));
            preparedStatement.setInt(4, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update details.
     *
     * @param details the player details to save
     */
    public static void saveMotto(PlayerDetails details) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET motto = ?, console_motto = ? WHERE id = ?", sqlConnection);
            preparedStatement.setString(1, details.getMotto());
            preparedStatement.setString(2, details.getConsoleMotto());
            preparedStatement.setInt(3, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update newsletter subscription.
     *
     * @param details the player details to save
     */
    public static void saveReceiveNews(PlayerDetails details) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET receive_email = ? WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, details.isReceiveNews() ? 1 : 0);
            preparedStatement.setInt(2, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update email.
     *
     * @param details Update the player email
     */
    public static void saveEmail(PlayerDetails details) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET email = ? WHERE id = ?", sqlConnection);
            preparedStatement.setString(1, details.getEmail());
            preparedStatement.setInt(2, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Update details.
     *
     * @param details the player details to save
     */
    public static void saveSubscription(PlayerDetails details) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE users SET club_subscribed = ?, club_expiration = ? WHERE id = ?", sqlConnection);
            preparedStatement.setLong(1, details.getFirstClubSubscription());
            preparedStatement.setLong(2, details.getClubExpiration());
            preparedStatement.setInt(3, details.getId());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Fill player data
     *
     * @param details the details
     * @param row the row
     * @throws SQLException the SQL exception
     */
    private static void fill(PlayerDetails details, ResultSet row) throws SQLException {
        // public void fill(int id, String username, String password, String figure, String poolFigure, int credits, String motto, String consoleMotto, String sex,
        // int tickets, int film, int rank, long lastOnline, long clubSubscribed, long clubExpiration, String badge, String badgeActive) {
        if (details == null) {
            return;
        }

        details.fill(row.getInt("id"), row.getString("username"), row.getString("figure"),
                row.getString("pool_figure"), row.getInt("credits"), row.getString("motto"),
                row.getString("console_motto"), row.getString("sex"), row.getInt("tickets"),
                row.getInt("film"), row.getInt("rank"), row.getLong("last_online"),
                row.getLong("club_subscribed"), row.getLong("club_expiration"), row.getLong("club_gift_due"),
                row.getString("badge"),
                row.getBoolean("badge_active"), row.getBoolean("allow_stalking"),
                row.getBoolean("allow_friend_requests"), row.getBoolean("sound_enabled"),
                row.getBoolean("tutorial_finished"), row.getInt("battleball_points"),
                row.getInt("snowstorm_points"),
                row.getInt("group_id"), row.getString("email"),row.getString("birthday"), row.getBoolean("receive_email"), row.getInt("last_streak"), row.getInt("login_streak"));
    }
}