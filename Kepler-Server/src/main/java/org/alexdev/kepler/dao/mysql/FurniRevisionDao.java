package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Furni published from the CMS's furni editor (table furni_versions, tools/migrations/update.1.52.sql): the highest
 * revision per sprite. The client puts it into dynamic.download.name.template's %revision%, so a new revision is a new
 * download URL and nobody keeps a cached cast.
 */
public class FurniRevisionDao {
    /**
     * Sprite -> revision for every furni with versions. Empty when the table doesn't exist yet.
     */
    public static Map<String, Integer> getRevisions() {
        Map<String, Integer> revisions = new LinkedHashMap<>();
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT sprite, MAX(revision) AS revision FROM furni_versions GROUP BY sprite", sqlConnection);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                revisions.put(resultSet.getString("sprite"), resultSet.getInt("revision"));
            }
        } catch (Exception e) {
            if (!String.valueOf(e.getMessage()).contains("furni_versions")) {
                Storage.logError(e);
            }
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return revisions;
    }
}
