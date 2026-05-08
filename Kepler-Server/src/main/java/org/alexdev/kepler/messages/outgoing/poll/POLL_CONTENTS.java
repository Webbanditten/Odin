package org.alexdev.kepler.messages.outgoing.poll;

import org.alexdev.kepler.game.polls.Poll;
import org.alexdev.kepler.game.polls.PollQuestion;
import org.alexdev.kepler.game.polls.PollQuestionOption;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.List;


public class POLL_CONTENTS extends MessageComposer {

    private Poll poll;

    public POLL_CONTENTS(Poll poll) {
        this.poll = poll;
    }

    @Override
    public void compose(NettyResponse response) {
        // tPollID
        response.writeInt(this.poll.getId());
        // tPollHeadLine
        response.writeString(this.poll.getHeadline());
        // tPollThankYou
        response.writeString(this.poll.getThankYou());

        List<PollQuestion> questions = this.poll.getQuestions();

        // Question count
        response.writeInt(questions.size());

        for (int i = 0; i < questions.size(); i++) {
            PollQuestion question = questions.get(i);

            // questionID
            response.writeInt(question.getId());
            // questionNumber (1-based)
            response.writeInt(i + 1);
            // questionType (client type integer)
            response.writeInt(question.getPollQuestionType().getClientType());
            // questionText
            response.writeString(question.getText());

            if (question.getPollQuestionType().isSelectionType()) {
                // tSelectionCount
                response.writeInt(question.getOptions().size());
                // minSelect
                response.writeInt(question.getMinSelect());
                // maxSelect
                response.writeInt(question.getMaxSelect());
                // Selection option texts
                for (PollQuestionOption option : question.getOptions()) {
                    response.writeString(option.getName());
                }
            }
        }
    }

    @Override
    public short getHeader() {
        return 317;
    }
}
