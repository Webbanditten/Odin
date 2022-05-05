package org.alexdev.kepler.game.polls;


public class PollTrigger {
    private int id;
    private int pollId;
    private Integer roomId;
    private Integer timeFrom;
    private Integer timeTo;

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

    public Integer getRoomId() {
        return roomId;
    }

    public Integer getTimeFrom() {
        return timeFrom;
    }

    public Integer getTimeTo() {
        return timeTo;
    }
}
