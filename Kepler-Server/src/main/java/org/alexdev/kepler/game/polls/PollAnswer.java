package org.alexdev.kepler.game.polls;


public class PollAnswer {
    private int id;
    private int pollQuestionId;
    private int userId;
    private int pollId;
    private String value;

    public PollAnswer(int pollQuestionId, int userId, int pollId, String value) {
        this.pollQuestionId = pollQuestionId;
        this.userId = userId;
        this.pollId = pollId;
        this.value = value;
    }

    public PollAnswer(int id, int pollQuestionId, int userId, int pollId, String value) {
        this.id = id;
        this.pollQuestionId = pollQuestionId;
        this.userId = userId;
        this.pollId = pollId;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public int getPollQuestionId() {
        return pollQuestionId;
    }

    public int getUserId() {
        return userId;
    }

    public int getPollId() {
        return pollId;
    }

    public String getValue() {
        return value;
    }
}
