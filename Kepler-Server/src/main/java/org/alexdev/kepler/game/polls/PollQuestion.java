package org.alexdev.kepler.game.polls;


public class PollQuestion {
    private int id;
    private int pollId;
    private PollQuestionType pollQuestionType;

    public PollQuestion(int id, int pollId, PollQuestionType pollQuestionType) {
        this.id = id;
        this.pollId = pollId;
        this.pollQuestionType = pollQuestionType;
    }

    public int getId() {
        return id;
    }

    public int getPollId() {
        return pollId;
    }

    public org.alexdev.kepler.game.polls.PollQuestionType getPollQuestionType() {
        return pollQuestionType;
    }
}
