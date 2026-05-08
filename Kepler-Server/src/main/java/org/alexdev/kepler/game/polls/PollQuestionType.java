package org.alexdev.kepler.game.polls;

public enum PollQuestionType {
    CHOICE,
    MULTI_CHOICE,
    TEXT;

    /**
     * Returns the client-facing integer type for this question type.
     * Client types: 1 = single selection, 2 = multi selection, 3/4 = open text
     */
    public int getClientType() {
        switch (this) {
            case CHOICE:
                return 1;
            case MULTI_CHOICE:
                return 2;
            case TEXT:
                return 3;
            default:
                return 3;
        }
    }

    /**
     * Whether this question type uses selection options.
     */
    public boolean isSelectionType() {
        return this == CHOICE || this == MULTI_CHOICE;
    }
}
