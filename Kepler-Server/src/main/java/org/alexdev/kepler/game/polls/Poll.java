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
        return PollDao.getPollQuestions(this.id);
    }

    public static void sendAvailablePoll(Player player) {

        List<PollTrigger> triggers = PollDao.getPollTriggers(player.getDetails().getId());
        for (PollTrigger trigger : triggers) {
            System.out.println("GOT TRIGGERS FOR POLLS");
            System.out.println(trigger.getPoll().headline);
            System.out.println("WAt: " + trigger.getTimeTo());
        }
        if(triggers.isEmpty()) return;

        List<PollTrigger> triggersForRoom = new ArrayList<>();
        List<PollTrigger> triggersWithNoRoom = new ArrayList<>();

        for (PollTrigger trigger : triggers) {
           if(trigger.getRoomId() != 0) {
               triggersForRoom.add(trigger);
           } else {
                triggersWithNoRoom.add(trigger);
           }
        }

        List<PollTrigger> actualTriggers = new ArrayList<>();


        for (PollTrigger trigger : triggersForRoom) {
            boolean hasTimeWindow = trigger.getTimeFrom() != 0 && trigger.getTimeTo() != 0;
            System.out.println("hastimewindow: " + hasTimeWindow);
            boolean isWithinTimeWindow = hasTimeWindow ? !(DateUtil.getCurrentTimeSeconds() >= trigger.getTimeFrom() && DateUtil.getCurrentTimeSeconds() <= trigger.getTimeTo()) : true;
            System.out.println("isWithinTime: " + isWithinTimeWindow);
            if(isWithinTimeWindow) {
                if(trigger.getRoomId() == player.getRoomUser().getRoom().getId()) {
                    System.out.println("ADDED TRIGGER TO ACUTAL TRIGGERS");
                    actualTriggers.add(trigger);
                } else if(trigger.getRoomId() == 0) {
                    System.out.println("ADDED TRIGGER TO ACUTAL TRIGGERS 2");
                    actualTriggers.add(trigger);
                }
            }
        }


        if(!actualTriggers.isEmpty()) {
            System.out.println("ADDED TRIGGER TO ACUTAL TRIGGERS");
            PollTrigger pollTrigger = actualTriggers.get(0);
            player.send(new POLL_OFFER(pollTrigger.getPoll().getId(), pollTrigger.getPoll().getDescription()));
        }

    }
}
