package org.alexdev.kepler.game.polls;

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
}
