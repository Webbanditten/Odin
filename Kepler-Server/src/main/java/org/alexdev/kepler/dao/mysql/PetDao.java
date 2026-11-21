package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.pets.PetDetails;
import org.alexdev.kepler.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

public class PetDao {
    public static void createPet(long databaseId, String name, String type, int race, String colour) throws SQLException {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO items_pets (item_id, name, type, race, colour, nature_positive, nature_negative, friendship, born, last_kip, last_eat, last_drink, last_playtoy, last_playuser) VALUES (?, ?, ? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", sqlConnection);
            preparedStatement.setLong(1, databaseId);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, type);
            preparedStatement.setInt(4, race);
            preparedStatement.setString(5, colour);
            // Nature: the status window's "Natur:" words. nature_positive = pet_frnd_0..11 (friendliness), nature_negative
            // = pet_enrg_0..11 (energy); both decide how the pet behaves (Pet#getActivity, Pet#getSociability).
            preparedStatement.setInt(6, ThreadLocalRandom.current().nextInt(0, Pet.NATURE_COUNT));
            preparedStatement.setInt(7, ThreadLocalRandom.current().nextInt(0, Pet.NATURE_COUNT));
            preparedStatement.setFloat(8, Pet.FRIENDSHIP_START);
            preparedStatement.setLong(9, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setLong(10, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setLong(11, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setLong(12, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setLong(13, DateUtil.getCurrentTimeSeconds());
            preparedStatement.setLong(14, DateUtil.getCurrentTimeSeconds());

            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
            throw e;
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static void saveCoordinates(int id, int x, int y, int rotation) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE items_pets SET x = ?, y = ?, rotation = ? WHERE id = ?", sqlConnection);
            preparedStatement.setInt(1, x);
            preparedStatement.setInt(2, y);
            preparedStatement.setInt(3, rotation);
            preparedStatement.setInt(4, id);
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Save a pet's needs, friendship and where it is (so a restart doesn't put it back where the room last unloaded).
     */
    public static void saveDetails(int id, PetDetails petDetails, int x, int y, int rotation) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("UPDATE items_pets SET last_kip = ?, last_eat = ?, last_drink = ?, last_playtoy = ?, last_playuser = ?, friendship = ?, x = ?, y = ?, rotation = ? WHERE id = ?", sqlConnection);
            preparedStatement.setLong(1, petDetails.getLastKip());
            preparedStatement.setLong(2, petDetails.getLastEat());
            preparedStatement.setLong(3, petDetails.getLastDrink());
            preparedStatement.setLong(4, petDetails.getLastPlayToy());
            preparedStatement.setLong(5, petDetails.getLastPlayUser());
            preparedStatement.setFloat(6, petDetails.getFriendship());
            preparedStatement.setInt(7, x);
            preparedStatement.setInt(8, y);
            preparedStatement.setInt(9, rotation);
            preparedStatement.setInt(10, id);
            preparedStatement.execute();
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    public static PetDetails getPetDetails(long itemId) {
        PetDetails petDetails = null;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet row = null;

        try {
            connection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM items_pets WHERE item_id = ?", connection);
            preparedStatement.setLong(1, itemId);
            row = preparedStatement.executeQuery();

            while (row.next()) {
                petDetails = new PetDetails(row.getInt("id"), row.getInt("item_id"), row.getString("name"),
                        row.getString("type"), row.getString("race"), row.getString("colour"), row.getInt("nature_positive"),
                        row.getInt("nature_negative"), row.getFloat("friendship"), row.getLong("born"), row.getLong("last_kip"),
                        row.getLong("last_eat"), row.getLong("last_drink"), row.getLong("last_playtoy"), row.getLong("last_playuser"),
                        row.getInt("x"), row.getInt("y"), row.getInt("rotation"));
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(row);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(connection);
        }

        return petDetails;
    }

    public static PetDetails getPetDetailsById(int itemId) {
        PetDetails petDetails = null;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet row = null;

        try {
            connection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM items_pets WHERE id = ?", connection);
            preparedStatement.setLong(1, itemId);
            row = preparedStatement.executeQuery();

            while (row.next()) {
                petDetails = new PetDetails(row.getInt("id"), row.getInt("item_id"), row.getString("name"),
                        row.getString("type"), row.getString("race"), row.getString("colour"), row.getInt("nature_positive"),
                        row.getInt("nature_negative"), row.getFloat("friendship"), row.getLong("born"), row.getLong("last_kip"),
                        row.getLong("last_eat"), row.getLong("last_drink"), row.getLong("last_playtoy"), row.getLong("last_playuser"),
                        row.getInt("x"), row.getInt("y"), row.getInt("rotation"));
            }
        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(row);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(connection);
        }

        return petDetails;
    }
}
