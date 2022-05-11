package org.alexdev.kepler.messages.outgoing.poll;

import org.alexdev.kepler.game.polls.Poll;
import org.alexdev.kepler.game.polls.PollQuestion;
import org.alexdev.kepler.game.polls.PollQuestionOption;
import org.alexdev.kepler.game.polls.PollQuestionType;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;
import org.alexdev.kepler.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

        // Question count
        response.writeInt(this.poll.getQuestions().size());

        List<PollQuestion> questions = this.poll.getQuestions();
        for (PollQuestion question : questions) {
            // questionID
            response.writeInt(question.getId());
            System.out.println("questionID: " + question.getId());
            // questionNumber
            response.writeInt(questions.indexOf(question)+1);
            System.out.println("questionNumber: " + question.getId());
            // questionType
            response.writeInt((question.getPollQuestionType() == PollQuestionType.CHOICE) ? 1 : 3);
            System.out.println("questionType: " + ((question.getPollQuestionType() == PollQuestionType.CHOICE) ? 1 : 3));
            // questionText
            response.writeString(question.getText());
            System.out.println("questionText: " + question.getText());
            if(question.getPollQuestionType() == PollQuestionType.CHOICE) {
                // tSelectionCount
                response.writeInt(question.getOptions().size());
                System.out.println("tSelectionCount: " + question.getOptions().size());
                // minSelect
                response.writeInt(question.getMinSelect());
                System.out.println("minSelect: " + question.getMinSelect());
                // maxSelect
                response.writeInt(question.getMaxSelect());
                System.out.println("maxSelect: " + question.getMaxSelect());
                // questionText
                for(PollQuestionOption option : question.getOptions()) {
                    response.writeString(option.getName());
                    System.out.println("questionText: " + option.getName());
                }
            }

        }

/*
        // questionID
        response.writeInt(1);
        // questionNumber
        response.writeInt(1);
        // questionType
        response.writeInt(1);
        // questionText
        response.writeString("æøå"); //response.writeString(StringUtil.charsetEncode("æøå"));
        // tSelectionCount
        response.writeInt(2);
        // minSelect
        response.writeInt(1);
        // maxSelect
        response.writeInt(1);
        // questionText
        response.writeString("Nej");
        response.writeString("Ja");



        // questionID
        response.writeInt(2);
        // questionNumber
        response.writeInt(2);
        // questionType
        response.writeInt(1);
        // questionText
        response.writeString("Vælg dine favorit mærker");
        // tSelectionCount
        response.writeInt(3);
        // minSelect
        response.writeInt(0);
        // maxSelect
        response.writeInt(3);
        // questionText
        response.writeString("Nokia");
        response.writeString("Sony");
        response.writeString("Apple");




        // questionID
        response.writeInt(3);
        // questionNumber
        response.writeInt(3);
        // questionType
        response.writeInt(3);
        // questionText
        response.writeString("Skriv noget"); //response.writeString(StringUtil.charsetEncode("æøå"));
*/

    }

    @Override
    public short getHeader() {
        return 317;
    }
}