package org.alexdev.kepler.game.polls;


public class PollTrigger {
    private int id;
    private int pollId;
    private int roomId;
    private int timeFrom;
    private int timeTo;

    public PollTrigger(int id, int pollId, int roomId, int timeFrom, int timeTo) {
        this.id = id;
        this.pollId = pollId;
        this.roomId = roomId;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
    }

    public int getId() {
        return id;
    }

    public int getPollId() {
        return pollId;
    }

    public int getRoomId() {
        return roomId;
    }

    public int getTimeFrom() {
        return timeFrom;
    }

    public int getTimeTo() {
        return timeTo;
    }
}
