package org.alexdev.kepler.game.pets;

import org.alexdev.kepler.util.config.GameConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What pets say and the words they answer to.
 *
 * The hotel's own external texts (the file the client loads, ISO-8859-1) have them:
 * {@code pet.saying.<mood>.<dog|cat|croco>.<n>} and {@code pet.hotwords.<command>}. Odin reads that file from the
 * setting "pet.texts.url" (on prod the asset host's address on the host itself) when it starts and on
 * ":reload texts" / a settings reload, so the texts stay in one place and can be changed without a release.
 * Without the setting, or when the file can't be read, pets use a few short lines of their own.
 */
public class PetTexts {
    private static final Logger log = LoggerFactory.getLogger(PetTexts.class);
    private static PetTexts instance;

    public enum Mood { GENERIC, EAT, PLAY, SLEEP, SNIFF, BEG, ANGRY }

    private static final String[] SPECIES = { "dog", "cat", "croco" };

    private volatile Map<String, List<String>> sayings = Map.of();
    private volatile Map<String, List<String>> hotwords = Map.of();
    private volatile String loadedFrom = null;

    private PetTexts() {
        this.load();
    }

    /**
     * Read the texts again (in the background), unless the address is the one already read and reread isn't forced.
     */
    public void reload(boolean force) {
        String url = GameConfiguration.getInstance().getString("pet.texts.url", "").trim();

        if (!force && Objects.equals(url, this.loadedFrom)) {
            return;
        }

        Thread thread = new Thread(this::load, "PetTexts");
        thread.setDaemon(true);
        thread.start();
    }

    private synchronized void load() {
        String url = GameConfiguration.getInstance().getString("pet.texts.url", "").trim();
        Map<String, List<String>> newSayings = new HashMap<>();
        Map<String, List<String>> newHotwords = new HashMap<>();

        if (!url.isEmpty()) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    String body = new String(response.body(), StandardCharsets.ISO_8859_1);

                    for (String line : body.split("\r\n|\r|\n")) {
                        int equals = line.indexOf('=');

                        if (equals < 1) {
                            continue;
                        }

                        String key = line.substring(0, equals).trim();
                        String value = line.substring(equals + 1).trim();

                        if (value.isEmpty()) {
                            continue;
                        }

                        if (key.startsWith("pet.saying.")) {
                            // pet.saying.<mood>.<species>.<n>
                            String[] parts = key.split("\\.");

                            if (parts.length >= 4) {
                                newSayings.computeIfAbsent(parts[2] + "." + parts[3], k -> new ArrayList<>()).add(value);
                            }
                        } else if (key.startsWith("pet.hotwords.")) {
                            String command = key.substring("pet.hotwords.".length());

                            for (String word : value.split("[,|]")) {
                                String normalised = PetManager.normalise(word);

                                if (!normalised.isEmpty()) {
                                    newHotwords.computeIfAbsent(command, k -> new ArrayList<>()).add(normalised);
                                }
                            }
                        }
                    }
                } else {
                    log.warn("Pet texts: {} answered {}", url, response.statusCode());
                }
            } catch (Exception ex) {
                log.warn("Pet texts: could not read {}: {}", url, ex.toString());
            }
        }

        this.sayings = newSayings;
        this.hotwords = newHotwords;
        this.loadedFrom = url;

        log.info("Loaded {} pet sayings and {} pet command words{}", newSayings.values().stream().mapToInt(List::size).sum(),
                newHotwords.values().stream().mapToInt(List::size).sum(), url.isEmpty() ? " (pet.texts.url not set)" : "");
    }

    /**
     * A random line for this mood and kind of pet, or null when there is none.
     */
    public String getSaying(Mood mood, String petType) {
        String species = speciesOf(petType);
        List<String> lines = this.sayings.get(mood.name().toLowerCase() + "." + species);

        if (lines == null || lines.isEmpty()) {
            lines = FALLBACK.get(mood.name().toLowerCase() + "." + species);
        }

        if (lines == null || lines.isEmpty()) {
            return null;
        }

        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }

    /**
     * The hotel's words for a command (normalised), e.g. "come_here".
     */
    public List<String> getHotwords(String command) {
        return this.hotwords.getOrDefault(command, List.of());
    }

    private static String speciesOf(String petType) {
        try {
            int type = Integer.parseInt(petType);

            if (type >= 0 && type < SPECIES.length) {
                return SPECIES[type];
            }
        } catch (NumberFormatException ignored) { }

        return SPECIES[0];
    }

    // Short lines of our own for when the hotel's texts can't be read.
    private static final Map<String, List<String>> FALLBACK = new ConcurrentHashMap<>();

    static {
        FALLBACK.put("generic.dog", List.of("Vov!", "Vov vov!", "Vuf?"));
        FALLBACK.put("eat.dog", List.of("Nam nam!", "*gnasker*"));
        FALLBACK.put("play.dog", List.of("Vov! Vov!", "*hopper rundt*"));
        FALLBACK.put("sleep.dog", List.of("Zzz...", "*snorker*"));
        FALLBACK.put("sniff.dog", List.of("*snuser*", "Snif snif..."));
        FALLBACK.put("beg.dog", List.of("*kigger bedende op*", "Vuf...?"));
        FALLBACK.put("angry.dog", List.of("Grrr!", "*knurrer*"));

        FALLBACK.put("generic.cat", List.of("Miav!", "Mjav?", "Purrr..."));
        FALLBACK.put("eat.cat", List.of("*slubrer*", "Nam!"));
        FALLBACK.put("play.cat", List.of("Miav! Miav!", "*jagter halen*"));
        FALLBACK.put("sleep.cat", List.of("Zzz...", "*spinder i søvne*"));
        FALLBACK.put("sniff.cat", List.of("*snuser*"));
        FALLBACK.put("beg.cat", List.of("Miiiav...?", "*stryger sig op ad benet*"));
        FALLBACK.put("angry.cat", List.of("Hvæs!", "*pjusker pelsen*"));

        FALLBACK.put("generic.croco", List.of("Grrr...", "*klapper med kæberne*"));
        FALLBACK.put("eat.croco", List.of("*SMASK*", "Nam!"));
        FALLBACK.put("play.croco", List.of("*vifter med halen*", "Rrraw!"));
        FALLBACK.put("sleep.croco", List.of("Zzz...", "*bobler i søvne*"));
        FALLBACK.put("sniff.croco", List.of("*snuser*"));
        FALLBACK.put("beg.croco", List.of("*kigger sultent*"));
        FALLBACK.put("angry.croco", List.of("RRRAW!", "*viser tænder*"));
    }

    public static PetTexts getInstance() {
        if (instance == null) {
            synchronized (PetTexts.class) {
                if (instance == null) {
                    instance = new PetTexts();
                }
            }
        }

        return instance;
    }
}
