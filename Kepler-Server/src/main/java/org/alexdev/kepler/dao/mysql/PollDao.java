package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.polls.*;

import java.sql.*;
import java.util.List;

public class PollDao {

    public static Poll getPoll(int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        Poll poll = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls WHERE id NOT IN (SELECT id FROM polls_offers where user_id = ?) limit 1;", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                poll = new Poll(
                        resultSet.getInt("id"),
                        resultSet.getString("headline"),
                        resultSet.getString("thank_you"),
                        resultSet.getString("description")
                );
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return poll;
    }

    public static List<PollQuestion> getPollQuestions(int pollId) {
        return null;
    }

    public static List<PollQuestionOption> getPollQuestionOptions(int pollId) {
        return null;
    }

    public static List<PollTrigger> getPollTriggers() {
        return null;
    }

    public static void addAnswer(PollAnswer answer) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO polls_answers (poll_question_id, value) VALUES (?, ?)", sqlConnection);

            preparedStatement.setInt(1, answer.getPollQuestionId());
            preparedStatement.setString(2, answer.getValue());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }


}
