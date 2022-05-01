package org.alexdev.kepler.game.polls;

import java.util.List;

public class Poll {
    private int id;
    private String headline;
    private String thankYou;
    private String description;
    private List<PollQuestion> questions;
    public Poll(int id, String headline, String thankYou, String description, List<PollQuestion> questions) {
        this.id = id;
        this.headline = headline;
        this.thankYou = thankYou;
        this.description = description;
        this.questions = questions;
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
        return questions;
    }
}
