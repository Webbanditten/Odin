package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.polls.*;

import java.sql.*;
import java.util.List;

public class PollDao {

    public static Poll getPoll(int pollId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        Poll poll = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls WHERE id = ? limit 1;", sqlConnection);
            preparedStatement.setInt(1, pollId);
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
        // SELECT * FROM polls_triggers WHERE poll_id NOT IN (SELECT poll_id FROM polls_offers where user_id = 1) limit 1
        return null;
    }

    public static List<PollQuestionOption> getPollQuestionOptions(int pollId) {
        return null;
    }

    public static List<PollTrigger> getPollTrigger() {
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
