package org.alexdev.kepler.game.pets;

import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.tasks.PetTask;
import org.alexdev.kepler.util.StringUtil;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Talking to pets. Anyone in the room who says a pet's name gets looked at; the owner, Habbos with rights and staff
 * who can control any room can also give it commands ({@link PetCommand}), which it obeys depending on friendship,
 * mood and nature (PetTask#hear).
 */
public class PetManager {
    private static PetManager instance;

    /**
     * Chat in a room with pets.
     *
     * @param player the Habbo who spoke
     * @param room the room
     * @param speech what they said (chat or shout)
     */
    public void handleSpeech(Player player, Room room, String speech) {
        List<Pet> pets = room.getEntityManager().getEntitiesByClass(Pet.class);

        if (pets.isEmpty()) {
            return;
        }

        String[] words = normalise(speech).split(" ");

        if (words.length == 0 || words[0].isEmpty()) {
            return;
        }

        boolean mayCommand = room.hasRights(player.getDetails().getId()) || player.hasFuse(Fuse.ANY_ROOM_CONTROLLER);

        for (Pet pet : pets) {
            PetTask task = pet.getRoomUser().getTask();

            if (task == null) {
                continue;
            }

            String name = normalise(pet.getDetails().getName());

            if (name.isEmpty()) {
                continue;
            }

            String commandText = null;

            if (words[0].equals(name)) {
                commandText = String.join(" ", Arrays.copyOfRange(words, 1, words.length));
            } else if (words.length > 1 && words[words.length - 1].equals(name)) {
                commandText = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));
            } else if (!Arrays.asList(words).contains(name)) {
                continue;
            }

            PetCommand command = commandText == null || !mayCommand ? null : findCommand(commandText);
            task.hear(player, command, mayCommand);
        }
    }

    /**
     * The command a text starts with, the longest phrase first ("gå væk" before "gå").
     */
    public static PetCommand findCommand(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        PetCommand best = null;
        int bestLength = 0;

        for (PetCommand command : PetCommand.values()) {
            for (String phrase : command.getPhrases()) {
                if (phrase.isEmpty() || phrase.length() <= bestLength) {
                    continue;
                }

                if (text.equals(phrase) || text.startsWith(phrase + " ")) {
                    best = command;
                    bestLength = phrase.length();
                }
            }
        }

        return best;
    }

    /**
     * Lower case, punctuation as spaces (the apostrophe of "gi' lyd" stays), single spaces: "Rex, SIT!" = "rex sit".
     */
    public static String normalise(String text) {
        if (text == null) {
            return "";
        }

        String lower = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());

        for (char c : lower.toCharArray()) {
            builder.append(Character.isLetterOrDigit(c) || c == '\'' ? c : ' ');
        }

        return builder.toString().trim().replaceAll("\\s+", " ");
    }

    /**
     * Get if the pet name is valid.
     *
     * @param ownerName the buyer's name
     * @param name the name of the pet
     */
    public boolean isValidName(String ownerName, String name) {
        if (name.contains(" ")) {
            return false;
        }

        if (name.length() > 15) {
            return false;
        }

        if (name.length() < 1) {
            return false;
        }

        // The name must be something chat can find (PetManager#normalise), so not only punctuation.
        if (normalise(name).isEmpty() || normalise(name).contains(" ")) {
            return false;
        }

        return !ownerName.equalsIgnoreCase(name);
    }

    public PetType getType(Pet pet) {
        switch (pet.getDetails().getType()) {
            case "0": {
                return PetType.DOG;
            }
            case "1": {
                return PetType.CAT;
            }
            case "2": {
                return PetType.CROC;
            }
        }

        return null;
    }

    /**
     * Get the {@link PetManager} instance
     *
     * @return the pet manager instance
     */
    public static PetManager getInstance() {
        if (instance == null) {
            instance = new PetManager();
        }

        return instance;
    }
}
