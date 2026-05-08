package org.alexdev.kepler.game.polls;

public class PollOffer {
    private int id;
    private int pollId;
    private int userId;

    public PollOffer(int id, int pollId, int userId) {
        this.id = id;
        this.pollId = pollId;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public int getPollId() {
        return pollId;
    }

    public int getUserId() {
        return userId;
    }
}
