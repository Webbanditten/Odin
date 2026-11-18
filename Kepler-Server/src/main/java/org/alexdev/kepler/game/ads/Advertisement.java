package org.alexdev.kepler.game.ads;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * An ad from the CMS's cms_ads table (Housekeeping > Ads). Odin serves the in-game placements: the billboard in public
 * rooms, the room loading window and the pool screen. The CMS serves the picture (/ads/img/id, which counts the view)
 * and the click (/ads/click/id, which counts it and redirects).
 */
public class Advertisement {
    private final int id;
    private final String placement;
    private final String fit;
    private final boolean hasClickUrl;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final int weight;
    private final Set<Integer> rooms;

    public Advertisement(int id, String placement, String fit, boolean hasClickUrl, LocalDateTime startsAt, LocalDateTime endsAt, int weight, Set<Integer> rooms) {
        this.id = id;
        this.placement = placement;
        this.fit = fit;
        this.hasClickUrl = hasClickUrl;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.weight = Math.max(1, weight);
        this.rooms = rooms;
    }

    public boolean isActive(LocalDateTime now) {
        return (this.startsAt == null || !this.startsAt.isAfter(now)) && (this.endsAt == null || this.endsAt.isAfter(now));
    }

    public int getId() {
        return id;
    }

    public String getPlacement() {
        return placement;
    }

    /** Billboards: "auto" (the CMS slants a flat picture to the room's frame) or the frame format it was made for. */
    public String getFit() {
        return fit;
    }

    public boolean hasClickUrl() {
        return hasClickUrl;
    }

    public int getWeight() {
        return weight;
    }

    /** Billboards: the rooms it is shown in; empty = every room with a billboard. */
    public Set<Integer> getRooms() {
        return rooms;
    }
}
