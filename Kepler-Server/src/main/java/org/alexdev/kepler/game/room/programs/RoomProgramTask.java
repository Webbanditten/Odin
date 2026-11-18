package org.alexdev.kepler.game.room.programs;

import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.outgoing.rooms.items.SHOWPROGRAM;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drives the shows of the public rooms whose client code waits for the server to change them (docs/room-programs.md):
 * Club Mammoth's and Café Solnedgang's disco floors, Club Orient's light shows, Club 57's mood light and Det orange
 * Rum's floor lamp. Ticks every 500 ms; every room.programs.seconds (45; 0 = never) each channel moves on. A staff
 * member can set a channel by hand (:program), which holds it until ":program auto".
 */
public class RoomProgramTask implements Runnable, RoomShows {
    public static final String TASK_NAME = "RoomProgramTask";
    private static final int TICKS_PER_SECOND = 2;

    /** One thing in the room the server changes: the client's SHOWPROGRAM dest and what it gets sent. */
    private abstract static class Channel {
        final String dest;
        String[] current;   // the last SHOWPROGRAM sent (what late joiners get)
        boolean held;       // set by hand (:program), the timer leaves it alone
        int ticksLeft;

        Channel(String dest) {
            this.dest = dest;
        }

        /** The next show, or null to send nothing this tick. */
        abstract String[] next(int ticksPerChange);
    }

    /** A numbered style 1..max (the disco engines: "discofloor 4"); a random different one each change. */
    private static class StyleChannel extends Channel {
        private final String command;
        private final int max;
        private int style;

        StyleChannel(String dest, String command, int max, int start) {
            super(dest);
            this.command = command;
            this.max = max;
            this.style = start;
            this.current = this.show();
        }

        private String[] show() {
            return this.command == null
                    ? new String[] { this.dest, String.valueOf(this.style) }
                    : new String[] { this.dest, this.command, String.valueOf(this.style) };
        }

        @Override
        String[] next(int ticksPerChange) {
            if (--this.ticksLeft > 0) {
                return null;
            }

            this.ticksLeft = ticksPerChange;
            int next = ThreadLocalRandom.current().nextInt(1, this.max);
            this.style = next >= this.style ? next + 1 : next; // any style but the current one
            return this.show();
        }
    }

    /** A colour that fades to the next of a list in steps ("ambient1 fade 330000"). */
    private static class FadeChannel extends Channel {
        private static final int FADE_TICKS = 10; // 5 seconds from one colour to the next

        private final String command;
        private final int[] colours;
        private int from, to, step = FADE_TICKS;

        FadeChannel(String dest, String command, int... colours) {
            super(dest);
            this.command = command;
            this.colours = colours;
            this.from = this.to = colours[0];
            this.current = new String[] { dest, command, hex(colours[0]) };
        }

        @Override
        String[] next(int ticksPerChange) {
            if (this.step < FADE_TICKS) {
                this.step++;
                return new String[] { this.dest, this.command, hex(mix(this.from, this.to, this.step / (double) FADE_TICKS)) };
            }

            if (--this.ticksLeft > 0) {
                return null;
            }

            this.ticksLeft = ticksPerChange;
            this.from = this.to;
            int next;

            do {
                next = this.colours[ThreadLocalRandom.current().nextInt(this.colours.length)];
            } while (next == this.from && this.colours.length > 1);

            this.to = next;
            this.step = 0;
            return null;
        }

        private static int mix(int a, int b, double t) {
            int r = (int) Math.round(((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
            int g = (int) Math.round(((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
            int bl = (int) Math.round((a & 255) + (((b & 255) - (a & 255)) * t));
            return (r << 16) | (g << 8) | bl;
        }

        private static String hex(int colour) {
            return String.format("%06X", colour & 0xFFFFFF);
        }
    }

    /** The show task for a room, or null when its model has nothing the server drives (docs/room-programs.md). */
    public static RoomProgramTask forRoom(Room room) {
        List<Channel> channels = channelsFor(room.getModel().getName());
        return channels.isEmpty() ? null : new RoomProgramTask(room, channels);
    }

    /** The channels of a room model, or none: what each room's client script understands. */
    private static List<Channel> channelsFor(String model) {
        List<Channel> channels = new ArrayList<>();

        switch (model) {
            // Club Mammoth: "club_mammoth Class" changeDiscoStyle, 9 floor patterns (the dest word is ignored).
            case "club_mammoth" -> channels.add(new StyleChannel("discofloor", null, 9, 1));
            // Café Solnedgang: the same disco engine, 6 patterns.
            case "sunset_cafe" -> channels.add(new StyleChannel("discofloor", null, 6, 1));
            // Club Orient: 10 light shows (the spotlights always turn; 2-10 switch and blink the lamps).
            case "orient" -> channels.add(new StyleChannel("lights", null, 10, 1));
            // Club 57's dance floor: show_ambient1 (ink 35, subtract) tints the whole room; the client starts at 330000.
            case "malja_bar_b" -> channels.add(new FadeChannel("ambient1", "fade",
                    0x330000, 0x003333, 0x330033, 0x000033, 0x333300, 0x003300, 0x220022));
            // Det orange Rum: the floor lamp's palette lattialamppu1..5.
            case "bar_b" -> channels.add(new StyleChannel("lamp", "setlamp", 5, 1));
            default -> { }
        }

        return channels;
    }

    private final Room room;
    private final Map<String, Channel> channels = new LinkedHashMap<>();

    private RoomProgramTask(Room room, List<Channel> channels) {
        this.room = room;
        int ticksPerChange = Math.max(1, ticksPerChange());

        for (Channel channel : channels) {
            channel.ticksLeft = ticksPerChange; // the room starts on the client's default; the first change comes later
            this.channels.put(channel.dest, channel);
        }
    }

    private static int ticksPerChange() {
        return GameConfiguration.getInstance().getInteger("room.programs.seconds") * TICKS_PER_SECOND;
    }

    @Override
    public synchronized void run() {
        int ticksPerChange = ticksPerChange();

        if (ticksPerChange <= 0) {
            return; // the shows stay as they are (a staff member can still set them)
        }

        for (Channel channel : this.channels.values()) {
            if (channel.held) {
                continue;
            }

            String[] show = channel.next(ticksPerChange);

            if (show != null) {
                channel.current = show;
                this.room.send(new SHOWPROGRAM(show));
            }
        }
    }

    /** Set a channel by hand (:program): it keeps that state until {@link #resume()}. False if the room has no such channel. */
    public synchronized boolean hold(String dest, String[] show) {
        Channel channel = this.channels.get(dest);

        if (channel == null) {
            return false;
        }

        channel.held = true;
        channel.current = show;
        return true;
    }

    /** Back to the timer for every channel. */
    public synchronized void resume() {
        for (Channel channel : this.channels.values()) {
            channel.held = false;
            channel.ticksLeft = 1;
        }
    }

    public synchronized List<String> describe() {
        List<String> lines = new ArrayList<>();

        for (Channel channel : this.channels.values()) {
            lines.add(String.join(" ", channel.current) + (channel.held ? " (held)" : ""));
        }

        return lines;
    }

    @Override
    public synchronized List<String[]> currentShows() {
        List<String[]> shows = new ArrayList<>();

        for (Channel channel : this.channels.values()) {
            shows.add(channel.current);
        }

        return shows;
    }
}
