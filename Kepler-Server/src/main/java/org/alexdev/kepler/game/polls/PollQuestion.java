package org.alexdev.kepler.game.polls;


import java.util.List;

public class PollQuestion {
    private int id;
    private int pollId;
    private PollQuestionType pollQuestionType;
    private List<PollQuestionOption> options;

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

    public List<PollQuestionOption> getOptions() {
        return options;
    }
    public void addOptions(List<PollQuestionOption> options) {
        this.options = options;
    }

    public PollQuestionType getPollQuestionType() {
        return pollQuestionType;
    }
}
