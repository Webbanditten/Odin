package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.ads.Advertisement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The CMS's ad tables (OdinCMS sql/migrations/2026-11-18_ads.sql): cms_ads, cms_ad_rooms (a billboard's rooms) and
 * cms_billboard_frames (each public room's billboard format). Missing tables mean no ads.
 */
public class AdvertisementsDao {
    private static final Logger log = LoggerFactory.getLogger(AdvertisementsDao.class);

    public static List<Advertisement> getAds() {
        List<Advertisement> ads = new ArrayList<>();
        Map<Integer, Set<Integer>> rooms = new HashMap<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();

            preparedStatement = Storage.getStorage().prepare("SELECT ad_id, room_id FROM cms_ad_rooms", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                rooms.computeIfAbsent(resultSet.getInt("ad_id"), k -> new HashSet<>()).add(resultSet.getInt("room_id"));
            }

            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);

            preparedStatement = Storage.getStorage().prepare("SELECT id, placement, fit, click_url, starts_at, ends_at, weight FROM cms_ads " +
                    "WHERE enabled = 1 AND placement IN ('room_billboard', 'room_loading', 'pool_screen')", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String clickUrl = resultSet.getString("click_url");

                ads.add(new Advertisement(id, resultSet.getString("placement"), resultSet.getString("fit"),
                        clickUrl != null && !clickUrl.isBlank(),
                        resultSet.getObject("starts_at", LocalDateTime.class), resultSet.getObject("ends_at", LocalDateTime.class),
                        resultSet.getInt("weight"), rooms.getOrDefault(id, Set.of())));
            }
        } catch (SQLException e) {
            log.warn("No ads: {}", e.getMessage());
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return ads;
    }

    /** Each public room's billboard frame: {format, dx, dy} (dx/dy move the picture onto frames whose centre is 1 px off). */
    public static Map<Integer, String[]> getBillboardFrames() {
        Map<Integer, String[]> formats = new HashMap<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT room_id, format, dx, dy FROM cms_billboard_frames", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                formats.put(resultSet.getInt("room_id"), new String[] { resultSet.getString("format"),
                        String.valueOf(resultSet.getInt("dx")), String.valueOf(resultSet.getInt("dy")) });
            }
        } catch (SQLException e) {
            log.warn("No billboard frames: {}", e.getMessage());
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return formats;
    }
}
