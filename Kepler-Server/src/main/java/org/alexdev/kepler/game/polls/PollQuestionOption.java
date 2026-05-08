package org.alexdev.kepler.game.polls;

public class PollQuestionOption {
    private int id;
    private String name;
    private int pollQuestionId;

    public PollQuestionOption(int id, String name, int pollQuestionId) {
        this.id = id;
        this.name = name;
        this.pollQuestionId = pollQuestionId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPollQuestionId() {
        return pollQuestionId;
    }
}
