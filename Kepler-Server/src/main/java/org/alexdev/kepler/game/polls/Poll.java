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
    private List<PollQuestion> questions;

    public Poll(int id, String headline, String thankYou, String description) {
        this.id = id;
        this.headline = headline;
        this.thankYou = thankYou;
        this.description = description;
        this.questions = null;
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

    /**
     * Loads and caches questions with their options from the database.
     * Subsequent calls return the cached list.
     */
    public List<PollQuestion> getQuestions() {
        if (this.questions == null) {
            this.questions = PollDao.getPollQuestions(this.id);
            for (PollQuestion question : this.questions) {
                question.addOptions(PollDao.getPollQuestionOptions(question.getId()));
            }
        }
        return this.questions;
    }

    /**
     * Sends an available poll offer to the player if one exists that they
     * haven't already seen, and that matches trigger conditions (room, time window).
     */
    public static void sendAvailablePoll(Player player) {
        List<PollTrigger> triggers = PollDao.getPollTriggers(player.getDetails().getId());
        if (triggers.isEmpty()) return;

        List<PollTrigger> matchingTriggers = new ArrayList<>();

        for (PollTrigger trigger : triggers) {
            boolean hasTimeWindow = trigger.getTimeFrom() != 0 && trigger.getTimeTo() != 0;

            if (hasTimeWindow) {
                long now = DateUtil.getCurrentTimeSeconds();
                // Skip this trigger if we are OUTSIDE the time window
                if (now < trigger.getTimeFrom() || now > trigger.getTimeTo()) {
                    continue;
                }
            }

            // Check room trigger: roomId 0 means "any room"
            if (trigger.getRoomId() == 0) {
                matchingTriggers.add(trigger);
            } else if (player.getRoomUser() != null
                    && player.getRoomUser().getRoom() != null
                    && trigger.getRoomId() == player.getRoomUser().getRoom().getId()) {
                matchingTriggers.add(trigger);
            }
        }

        if (!matchingTriggers.isEmpty()) {
            PollTrigger pollTrigger = matchingTriggers.get(0);
            player.send(new POLL_OFFER(pollTrigger.getPoll().getId(), pollTrigger.getPoll().getDescription()));
        }
    }
}
