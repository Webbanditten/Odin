package org.alexdev.kepler.game.polls;


public class PollAnswer {
    private int id;
    private int pollQuestionId;
    private String value;

    public PollAnswer(int id, int pollQuestionId, String value) {
        this.id = id;
        this.pollQuestionId = pollQuestionId;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public int getPollQuestionId() {
        return pollQuestionId;
    }

    public String getValue() {
        return value;
    }
}
