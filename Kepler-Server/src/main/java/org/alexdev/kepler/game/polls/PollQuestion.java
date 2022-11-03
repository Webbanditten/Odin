package org.alexdev.kepler.game.polls;


import java.util.List;

public class PollQuestion {
    private int id;
    private int pollId;
    private PollQuestionType pollQuestionType;
    private List<PollQuestionOption> options;
    private String text;
    private int maxSelect;
    private int minSelect;

    public PollQuestion(int id, int pollId, PollQuestionType pollQuestionType, String text, int minSelect, int maxSelect) {
        this.id = id;
        this.pollId = pollId;
        this.pollQuestionType = pollQuestionType;
        this.text = text;
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
    }

    public int getId() {
        return id;
    }

    public int getPollId() {
        return pollId;
    }
    public int getMaxSelect() {
        return maxSelect;
    }
    public int getMinSelect() {
        return minSelect;
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
