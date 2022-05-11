package org.alexdev.kepler.game.polls;

import org.alexdev.kepler.dao.mysql.PollDao;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.poll.POLL_OFFER;
import org.alexdev.kepler.util.DateUtil;

import java.util.ArrayList;
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
        List<PollQuestion> questions = PollDao.getPollQuestions(this.id);
        for (PollQuestion question : questions) {
            question.addOptions(PollDao.getPollQuestionOptions(question.getId()));
        }
        return questions;
    }

    public static void sendAvailablePoll(Player player) {

        List<PollTrigger> triggers = PollDao.getPollTriggers(player.getDetails().getId());
        if(triggers.isEmpty()) return;

        List<PollTrigger> actualTriggers = new ArrayList<>();

        for (PollTrigger trigger : triggers) {
            boolean hasTimeWindow = trigger.getTimeFrom() != 0 && trigger.getTimeTo() != 0;
            boolean isWithinTimeWindow = !hasTimeWindow || !(DateUtil.getCurrentTimeSeconds() >= trigger.getTimeFrom() && DateUtil.getCurrentTimeSeconds() <= trigger.getTimeTo());
            if(isWithinTimeWindow) {
                if(trigger.getRoomId() == player.getRoomUser().getRoom().getId()) {
                    actualTriggers.add(trigger);
                } else if(trigger.getRoomId() == 0) {
                    actualTriggers.add(trigger);
                }
            }
        }

        if(!actualTriggers.isEmpty()) {
            PollTrigger pollTrigger = actualTriggers.get(0);
            player.send(new POLL_OFFER(pollTrigger.getPoll().getId(), pollTrigger.getPoll().getDescription()));
        }

    }
}
