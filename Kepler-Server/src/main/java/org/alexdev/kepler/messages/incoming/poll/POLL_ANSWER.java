package org.alexdev.kepler.messages.incoming.poll;

import org.alexdev.kepler.dao.mysql.PollDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.polls.*;
import org.alexdev.kepler.messages.outgoing.poll.POLL_ERROR;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

import java.sql.SQLException;
import java.util.List;

public class POLL_ANSWER implements MessageEvent {

    @Override
    public void handle(Player player, NettyRequest reader) throws SQLException {
        if (!player.isLoggedIn()) {
            return;
        }

        int pollId = reader.readInt();
        int questionId = reader.readInt();
        int userId = player.getDetails().getId();

        // Validate the question exists
        PollQuestion question = PollDao.getQuestion(questionId);
        if (question == null) {
            player.send(new POLL_ERROR());
            return;
        }

        // Check if this user already answered this question (prevent duplicates)
        if (PollDao.hasUserAnswered(questionId, userId)) {
            return;
        }

        if (question.getPollQuestionType().isSelectionType()) {
            // Selection-based answer: client sends count + indices (1-based)
            int amountSelected = reader.readInt();
            List<PollQuestionOption> questionOptions = PollDao.getPollQuestionOptions(question.getId());

            if (questionOptions == null || questionOptions.isEmpty()) {
                player.send(new POLL_ERROR());
                return;
            }

            for (int i = 0; i < amountSelected; i++) {
                int selectedIndex = reader.readInt(); // 1-based index from client

                if (selectedIndex < 1 || selectedIndex > questionOptions.size()) {
                    continue; // Skip invalid indices
                }

                PollQuestionOption option = questionOptions.get(selectedIndex - 1);
                PollDao.addAnswer(new PollAnswer(question.getId(), userId, pollId, Integer.toString(option.getId())));
            }
        } else {
            // Free-text answer
            String text = reader.readString();
            PollDao.addAnswer(new PollAnswer(question.getId(), userId, pollId, text));
        }
    }
}
