package org.alexdev.kepler.dao.mysql;

import org.alexdev.kepler.dao.Storage;
import org.alexdev.kepler.game.polls.*;

import java.sql.*;
import java.util.ArrayList;
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
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<PollQuestion> questions = new ArrayList<>();

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls_questions WHERE poll_id = ?", sqlConnection);
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

    public static List<PollQuestionOption> getPollQuestionOptions(int questionId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<PollQuestionOption> questionOptions = new ArrayList<>();

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls_questions_options WHERE poll_question_id = ?", sqlConnection);
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

    public static List<PollTrigger> getPollTriggers(int userId) {
        List<PollTrigger> triggers = new ArrayList<>();

        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT\n" +
                    "pt.id as trigger_id,\n" +
                    "pt.room,\n" +
                    "pt.time_from,\n" +
                    "pt.time_to,\n" +
                    "p.id as poll_id,\n" +
                    "p.headline,\n" +
                    "p.thank_you,\n" +
                    "p.description\n" +
                    "FROM polls_triggers as pt\n" +
                    "LEFT JOIN polls as p on pt.poll_id = p.id\n" +
                    "WHERE p.id NOT IN (SELECT po.poll_id FROM polls_offers as po where po.user_id = ?);", sqlConnection);
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

    public static void addOffer(int pollId, int userId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("INSERT INTO polls_offers (poll_Id, user_id) VALUES (?, ?)", sqlConnection);

            preparedStatement.setInt(1, pollId);
            preparedStatement.setInt(2, userId);
            preparedStatement.execute();

        } catch (Exception e) {
            Storage.logError(e);
        } finally {
            Storage.closeSilently(preparedStatement);
            Storage.closeSilently(sqlConnection);
        }
    }


    public static PollQuestion getQuestion(int questionId) {
        Connection sqlConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        PollQuestion question = null;

        try {
            sqlConnection = Storage.getStorage().getConnection();
            preparedStatement = Storage.getStorage().prepare("SELECT * FROM polls_questions WHERE id = ? limit 1;", sqlConnection);
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
