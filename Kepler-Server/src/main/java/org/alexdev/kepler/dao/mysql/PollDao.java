package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.polls.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PollDao {

    /**
     * Get a poll by its ID.
     */
    public static Poll getPoll(int pollId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        Poll poll = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls WHERE id = ? AND enabled = 1 LIMIT 1", sqlConnection);
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

    /**
     * Get all questions for a poll, ordered by the `order` column.
     */
    public static List<PollQuestion> getPollQuestions(int pollId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<PollQuestion> questions = new ArrayList<>();

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls_questions WHERE poll_id = ? ORDER BY `order` ASC, id ASC", sqlConnection);
            preparedStatement.setInt(1, pollId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                questions.add(new PollQuestion(
                        resultSet.getInt("id"),
                        resultSet.getInt("poll_id"),
                        PollQuestionType.valueOf(resultSet.getString("type")),
                        resultSet.getString("text"),
                        resultSet.getInt("min_select"),
                        resultSet.getInt("max_select")
                ));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return questions;
    }

    /**
     * Get all options for a question, ordered by the `order` column.
     */
    public static List<PollQuestionOption> getPollQuestionOptions(int questionId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<PollQuestionOption> questionOptions = new ArrayList<>();

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls_questions_options WHERE poll_question_id = ? ORDER BY `order` ASC, id ASC", sqlConnection);
            preparedStatement.setInt(1, questionId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                questionOptions.add(new PollQuestionOption(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("poll_question_id")
                ));
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return questionOptions;
    }

    /**
     * Get available poll triggers for a user (polls they haven't already been offered).
     * Only returns triggers for enabled polls.
     */
    public static List<PollTrigger> getPollTriggers(int userId) {
        List<PollTrigger> triggers = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                    "SELECT " +
                    "pt.id as trigger_id, " +
                    "pt.room, " +
                    "pt.time_from, " +
                    "pt.time_to, " +
                    "p.id as poll_id, " +
                    "p.headline, " +
                    "p.thank_you, " +
                    "p.description " +
                    "FROM polls_triggers as pt " +
                    "LEFT JOIN polls as p ON pt.poll_id = p.id " +
                    "WHERE p.enabled = 1 " +
                    "AND p.id NOT IN (SELECT po.poll_id FROM polls_offers as po WHERE po.user_id = ?)", sqlConnection);
            preparedStatement.setInt(1, userId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                PollTrigger trigger = new PollTrigger(
                        resultSet.getInt("trigger_id"),
                        resultSet.getInt("poll_id"),
                        resultSet.getInt("room"),
                        resultSet.getInt("time_from"),
                        resultSet.getInt("time_to"),
                        new Poll(
                                resultSet.getInt("poll_id"),
                                resultSet.getString("headline"),
                                resultSet.getString("thank_you"),
                                resultSet.getString("description")
                        )
                );
                triggers.add(trigger);
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return triggers;
    }

    /**
     * Save a poll answer to the database.
     */
    public static void addAnswer(PollAnswer answer) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                    "INSERT IGNORE INTO polls_answers (poll_question_id, user_id, poll_id, value) VALUES (?, ?, ?, ?)", sqlConnection);

            preparedStatement.setInt(1, answer.getPollQuestionId());
            preparedStatement.setInt(2, answer.getUserId());
            preparedStatement.setInt(3, answer.getPollId());
            preparedStatement.setString(4, answer.getValue());
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Record a poll offer (accept or reject) to prevent re-offering.
     */
    public static void addOffer(int pollId, int userId, String status) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                    "INSERT INTO polls_offers (poll_id, user_id, status) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE status = VALUES(status)", sqlConnection);

            preparedStatement.setInt(1, pollId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setString(3, status);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }

    /**
     * Check if a user has already answered a specific question.
     */
    public static boolean hasUserAnswered(int questionId, int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare(
                    "SELECT COUNT(*) FROM polls_answers WHERE poll_question_id = ? AND user_id = ?", sqlConnection);
            preparedStatement.setInt(1, questionId);
            preparedStatement.setInt(2, userId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return false;
    }

    /**
     * Get a single question by its ID.
     */
    public static PollQuestion getQuestion(int questionId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        PollQuestion question = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls_questions WHERE id = ? LIMIT 1", sqlConnection);
            preparedStatement.setInt(1, questionId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                question = new PollQuestion(
                        resultSet.getInt("id"),
                        resultSet.getInt("poll_id"),
                        PollQuestionType.valueOf(resultSet.getString("type")),
                        resultSet.getString("text"),
                        resultSet.getInt("min_select"),
                        resultSet.getInt("max_select")
                );
            }

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(resultSet);
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }

        return question;
    }
}
