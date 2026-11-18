package org.alexdev.kepler.game.room.tasks;

import java.util.List;
import java.util.ArrayList;
import org.alexdev.kepler.game.room.programs.RoomShows;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.rooms.items.SHOWPROGRAM;

import java.util.concurrent.ThreadLocalRandom;

public class SpaceCafeTask implements Runnable, RoomShows {
    public static final int FLIPBOARD_TIME = 60;
    public static final int LIGHT_TIME = 5;

    // The room has show_flipflop0..7 (stacked, 0 on top); "flipflop8" logged "Sprite not found" and left the board blank.
    private static final int[] FLIPBOARD_ORDER  = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
    private final Room room;

    private int stageIndex;
    private long timeToNextStage;
    private long timeToNextLight;

    private int shownBoard = 0;

    private int firstColour;
    private int secondColour;
    private int thirdColour;

    public SpaceCafeTask(Room room) {
        this.room = room;
        this.stageIndex = 2;
        this.timeToNextStage = convertToInterval(FLIPBOARD_TIME);
        this.timeToNextLight = convertToInterval(LIGHT_TIME);
    }

    @Override
    public void run() {
        if (timeToNextStage > 0) {
            timeToNextStage--;
        }

        if (this.timeToNextStage == 0) {
            processBoardFlip();
        }

        if (this.timeToNextLight > 0) {
            this.timeToNextLight--;
        }

        if (this.timeToNextLight == 0) {
            this.processLightColor();
        }
    }

    private void processLightColor() {
        int[] numbers = new int[] {4, 1, 2};

        int tempFirstColour = findNewColour(this.firstColour, numbers);
        int tempSecondColour = findNewColour(this.secondColour, numbers);
        int tempThirdColour = findNewColour(this.thirdColour, numbers);

        if (tempFirstColour != this.firstColour) {
            this.firstColour = tempFirstColour;
            this.room.send(new SHOWPROGRAM(new String[]{"lmpa", "ufol", String.valueOf(this.firstColour)}));
            this.room.send(new SHOWPROGRAM(new String[]{"liga", "litecol", String.valueOf(this.firstColour)}));
        }

        if (tempSecondColour != this.secondColour) {
            this.secondColour = tempSecondColour;
            this.room.send(new SHOWPROGRAM(new String[]{"lmpb", "ufol", String.valueOf(this.secondColour)}));
            this.room.send(new SHOWPROGRAM(new String[]{"ligb", "litecol", String.valueOf(this.secondColour)}));
        }

        if (tempThirdColour != this.thirdColour) {
            this.thirdColour = tempThirdColour;
            this.room.send(new SHOWPROGRAM(new String[]{"lmpc", "ufol", String.valueOf(this.thirdColour)}));
            this.room.send(new SHOWPROGRAM(new String[]{"ligc", "litecol", String.valueOf(this.thirdColour)}));
        }

        this.timeToNextLight = convertToInterval(LIGHT_TIME);
    }

    private void processBoardFlip() {
        this.stageIndex++;

        if (this.stageIndex >= FLIPBOARD_ORDER.length) {
            this.stageIndex = 0;
        }

        int previousBoard = this.stageIndex;
        int nextBoard = (previousBoard + 1) >= FLIPBOARD_ORDER.length ? 0 : previousBoard + 1;

        room.send(new SHOWPROGRAM(new String[]{"flipflop" + FLIPBOARD_ORDER[previousBoard], "visible", String.valueOf(0)}));
        room.send(new SHOWPROGRAM(new String[]{"flipflop" + FLIPBOARD_ORDER[nextBoard], "visible", String.valueOf(1)}));

        this.shownBoard = nextBoard;
        boolean isStillBoard = (previousBoard == 2 && nextBoard == 3) || (previousBoard == 5 && nextBoard == 6) || (previousBoard == 7 && nextBoard == 0);

        if (isStillBoard) {
            this.timeToNextStage = convertToInterval(FLIPBOARD_TIME);
        } else {
            this.timeToNextStage = 1;
        }
    }

    /** The boards (only the shown one visible) and the three lamps, for a player who walks in. */
    @Override
    public List<String[]> currentShows() {
        List<String[]> shows = new ArrayList<>();

        for (int board : FLIPBOARD_ORDER) {
            shows.add(new String[] { "flipflop" + board, "visible", board == FLIPBOARD_ORDER[this.shownBoard] ? "1" : "0" });
        }

        String[][] lamps = { { "lmpa", "liga" }, { "lmpb", "ligb" }, { "lmpc", "ligc" } };
        int[] colours = { this.firstColour, this.secondColour, this.thirdColour };

        for (int i = 0; i < lamps.length; i++) {
            if (colours[i] > 0) {
                shows.add(new String[] { lamps[i][0], "ufol", String.valueOf(colours[i]) });
                shows.add(new String[] { lamps[i][1], "litecol", String.valueOf(colours[i]) });
            }
        }

        return shows;
    }

    public int findNewColour(int currentColour, int[] selection) {
        return  selection[ThreadLocalRandom.current().nextInt(0, selection.length)];
        /*int newColour = currentColour;

        while (newColour == currentColour) {
            newColour = selection[ThreadLocalRandom.current().nextInt(0, selection.length)];
        }

        return newColour;*/
    }

    public static int convertToInterval(int seconds) {
        return seconds * 2;
    }
}
