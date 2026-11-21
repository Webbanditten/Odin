package org.alexdev.kepler.game.pets;

import java.util.ArrayList;
import java.util.List;

/**
 * What a pet can be told: "&lt;name&gt; &lt;command&gt;" (or "&lt;command&gt; &lt;name&gt;") in the room's chat. Each command
 * answers to the hotel's own word for it (pet.hotwords.&lt;key&gt; in the external texts, see PetTexts) and to a few
 * everyday Danish and English ones.
 */
public enum PetCommand {
    SIT("sit", false, "sit", "sid", "sæt dig", "sid ned"),
    LIE("lie", false, "læg dig", "læg dig ned", "lig ned", "lig", "dæk", "lie down", "lay", "lie"),
    PLAY_DEAD("play_dead", true, "spil død", "død", "play dead", "dead"),
    JUMP("jump", true, "hop op", "hop", "spring", "jump"),
    VOICE("voice", false, "gi' lyd", "gi lyd", "giv lyd", "sig noget", "tal", "lyd", "speak"),
    BEG("beg", false, "tig", "bed pænt", "beg"),
    COME_HERE("come_here", true, "kom her", "kom", "her", "come here", "come over", "come", "heel"),
    FOLLOW_ME("follow_me", true, "følg mig", "følg", "følg med", "follow me", "follow"),
    GO_AWAY("go_away", true, "gå væk", "gå", "smut", "forsvind", "go away", "go"),
    NEST("nest", false, "sov", "i kurven", "kurv", "godnat", "god nat", "sleep", "go to bed"),
    GOOD("good", false, "godt", "godt klaret", "dygtig", "flink", "god hund", "god kat", "good", "good boy", "good girl"),
    BAD("bad", false, "fy", "nej", "slemt", "bad", "no"),
    EAT("eat", false, "spis", "mad", "eat"),
    DRINK("drink", false, "drik", "vand", "drink");

    private final String hotwordKey;
    private final boolean active;
    private final String[] builtIn;

    PetCommand(String hotwordKey, boolean active, String... builtIn) {
        this.hotwordKey = hotwordKey;
        this.active = active;
        this.builtIn = builtIn;
    }

    /** The key in pet.hotwords.&lt;key&gt;. */
    public String getHotwordKey() {
        return hotwordKey;
    }

    /** Needs some energy: a tired pet would rather not. */
    public boolean isActive() {
        return active;
    }

    /** Every phrase this command answers to, normalised like chat (PetManager#normalise). */
    public List<String> getPhrases() {
        List<String> phrases = new ArrayList<>(PetTexts.getInstance().getHotwords(this.hotwordKey));

        for (String word : this.builtIn) {
            String normalised = PetManager.normalise(word);

            if (!phrases.contains(normalised)) {
                phrases.add(normalised);
            }
        }

        return phrases;
    }
}
