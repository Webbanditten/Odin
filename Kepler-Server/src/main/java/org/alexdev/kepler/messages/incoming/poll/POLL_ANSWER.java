package org.alexdev.kepler.messages.incoming.poll;

import org.alexdev.kepler.dao.mysql.PollDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.polls.*;
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
        Integer id = reader.readInt();
        Integer questionId = reader.readInt();

        System.out.println("ID: " + id);
        System.out.println("questionId: " + questionId);

        PollQuestion question = PollDao.getQuestion(questionId);
        if(question == null) return;

        List<PollQuestionOption> questionOptions = PollDao.getPollQuestionOptions(question.getId());
        if(question.getPollQuestionType() == PollQuestionType.CHOICE) {
            // amountSelected
            Integer amountSelected = reader.readInt();
            for(Integer i = 0; i < amountSelected; i++) {
                Integer selectedIndex = reader.readInt();
                if(questionOptions != null) {
                    PollQuestionOption option = questionOptions.get(selectedIndex-1);
                    if(option != null) {
                        PollDao.addAnswer(new PollAnswer(question.getId(), Integer.toString(option.getId())));
                    }
                }
            }
        } else {
            String text = reader.readString();
            // THIS IS WRONG FIX IT LATER
            PollDao.addAnswer(new PollAnswer(question.getId(), text));
        }
    }
}
