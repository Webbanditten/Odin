package org.alexdev.kepler.game.ads;

import org.alexdev.kepler.dao.mysql.AdvertisementsDao;
import org.alexdev.kepler.util.config.GameConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * The in-game ads (cms_ads, managed in the CMS's Housekeeping > Ads). Loaded at start and on the reload_ads command the CMS
 * sends after every change; start and end dates are checked when an ad is picked. The URLs point at the CMS (setting
 * ads.url, e.g. https://habbo-danmark.com); without it Odin sends no ads.
 */
public class AdManager {
    public static final String BILLBOARD = "room_billboard";
    public static final String ROOM_LOADING = "room_loading";
    public static final String POOL_SCREEN = "pool_screen";
    public static final String AUTO = "auto";

    private static final Logger log = LoggerFactory.getLogger(AdManager.class);
    private static volatile AdManager instance;

    private final List<Advertisement> ads;
    private final Map<Integer, String[]> billboardFrames;

    private AdManager() {
        this.billboardFrames = AdvertisementsDao.getBillboardFrames();
        this.ads = AdvertisementsDao.getAds();
        log.info("Loaded {} in-game ads and {} billboard frames", this.ads.size(), this.billboardFrames.size());
    }

    public static AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }

        return instance;
    }

    /** Reloads the ads from the database (reload_ads). */
    public static void reset() {
        instance = new AdManager();
    }

    /** The CMS address the ad URLs start with (ads.url), or null when in-game ads are off. */
    public static String getBaseUrl() {
        String url = GameConfiguration.getInstance().getString("ads.url", "").trim();

        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url.startsWith("http") ? url : null;
    }

    /** The public room's billboard frame {format (small_left, ...), dx, dy}, or null if it has none. */
    public String[] getBillboardFrame(int roomId) {
        return this.billboardFrames.get(roomId);
    }

    /** A billboard ad for the room: one made for its frame format, or a flat one the CMS slants. */
    public Advertisement pickBillboard(int roomId) {
        String[] frame = this.billboardFrames.get(roomId);

        if (frame == null) {
            return null;
        }

        String format = frame[0];

        return pick(this.ads.stream()
                .filter(ad -> ad.getPlacement().equals(BILLBOARD))
                .filter(ad -> ad.getRooms().isEmpty() || ad.getRooms().contains(roomId))
                .filter(ad -> ad.getFit().equals(AUTO) || ad.getFit().equals(format))
                .collect(Collectors.toList()));
    }

    public Advertisement pick(String placement) {
        return pick(this.ads.stream().filter(ad -> ad.getPlacement().equals(placement)).collect(Collectors.toList()));
    }

    private static Advertisement pick(List<Advertisement> candidates) {
        LocalDateTime now = LocalDateTime.now();
        List<Advertisement> active = candidates.stream().filter(ad -> ad.isActive(now)).collect(Collectors.toList());

        if (active.isEmpty()) {
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(active.stream().mapToInt(Advertisement::getWeight).sum());

        for (Advertisement ad : active) {
            roll -= ad.getWeight();

            if (roll < 0) {
                return ad;
            }
        }

        return active.get(active.size() - 1);
    }

    /** The picture's URL; a billboard gets the room's frame (format, and dx/dy when the frame's centre is 1 px off). */
    public static String getImageUrl(String baseUrl, Advertisement ad, String[] frame) {
        String url = baseUrl + "/ads/img/" + ad.getId();

        if (ad.getPlacement().equals(BILLBOARD) && frame != null) {
            url += "?f=" + frame[0];

            if (!frame[1].equals("0") || !frame[2].equals("0")) {
                url += "&dx=" + frame[1] + "&dy=" + frame[2];
            }
        }

        return url;
    }

    /** The click URL (the CMS counts it and redirects), or "" for an ad without a link. */
    public static String getClickUrl(String baseUrl, Advertisement ad) {
        return ad.hasClickUrl() ? baseUrl + "/ads/click/" + ad.getId() : "";
    }
}
