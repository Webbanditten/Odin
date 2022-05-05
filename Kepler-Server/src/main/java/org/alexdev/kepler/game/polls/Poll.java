package org.alexdev.kepler.game.polls;

import org.alexdev.kepler.dao.mysql.PollDao;

import java.util.List;

public class Poll {
    private int id;
    private String headline;
    private String thankYou;
    private String description;

    public Poll(int id, String headline, String thankYou, String description) {
        this.id = id;
        this.headline = headline;
        this.thankYou = thankYou;
        this.description = description;
    }

    public int getId() {
        return id;
    }
    public String getHeadline() {
        return headline;
    }
    public String getThankYou() {
        return thankYou;
    }
    public String getDescription() {
        return description;
    }
    public List<PollQuestion> getQuestions() {
        return PollDao.getPollQuestions(this.id);
    }

    public void sendAvailablePoll(int userId, Integer roomId) {
        List<PollTrigger> triggers = PollDao.getPollTrigger();

        for (PollTrigger trigger : triggers) {
            if(trigger.getTimeFrom() != null && trigger.getTimeTo() != null) {
                if(trigger.getRoomId() != null) {

                } else {

                }
            } else {
                if(trigger.getRoomId() != null) {

                } else {

                }
            }

        }

    }
}
