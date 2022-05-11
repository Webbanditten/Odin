package org.alexdev.kepler.game.polls;


import java.util.List;

public class PollQuestion {
    private int id;
    private int pollId;
    private PollQuestionType pollQuestionType;
    private List<PollQuestionOption> options;
    private String text;

    public PollQuestion(int id, int pollId, PollQuestionType pollQuestionType, String text) {
        this.id = id;
        this.pollId = pollId;
        this.pollQuestionType = pollQuestionType;
        this.text = text;
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
    public String getText() { return text; }
}
