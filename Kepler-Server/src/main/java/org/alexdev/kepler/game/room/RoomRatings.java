package org.alexdev.kepler.game.room;

import org.alexdev.kepler.dao.mysql.RoomDao;
import org.alexdev.kepler.dao.mysql.RoomVoteDao;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rolling room ratings (migration update.1.47.sql). A vote counts for the day's best rooms for 24 hours and for the
 * week's for 7 days after it was cast, and forever in the room's rating; its voter may vote on the room again after
 * room.rating.vote.days (7). The navigator shows the rankings in the guest room categories whose ranking is
 * daily / weekly / alltime (rooms_categories.ranking), and "Anbefalede Rum" are the week's best open rooms.
 * Lists are cached for a minute and cleared by every vote.
 */
public class RoomRatings {
    public static final String DAILY = "daily";
    public static final String WEEKLY = "weekly";
    public static final String ALL_TIME = "alltime";

    /** Rooms listed in a ranking category. */
    public static final int CATEGORY_SIZE = 30;
    private static final long CACHE_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private static RoomRatings instance;

    private record Ranking(List<Room> rooms, long loadedAt) { }

    private final Map<String, Ranking> cache = new ConcurrentHashMap<>();

    /**
     * How long a vote blocks its voter from voting on the same room again (seconds).
     */
    public long getVoteInterval() {
        int days = GameConfiguration.getInstance().getInteger("room.rating.vote.days");
        return TimeUnit.DAYS.toSeconds(days > 0 ? days : 7);
    }

    /**
     * The rooms of a ranking (daily, weekly or alltime), best first, as the database last saw them (use
     * RoomManager.replaceQueryRooms for the loaded ones).
     */
    public List<Room> getRooms(String ranking) {
        Ranking cached = this.cache.get(ranking);

        if (cached != null && System.currentTimeMillis() - cached.loadedAt() < CACHE_MILLIS) {
            return cached.rooms();
        }

        List<Integer> ids;
        long now = DateUtil.getCurrentTimeSeconds();

        switch (ranking) {
            case DAILY -> ids = RoomVoteDao.getBestRoomsSince(now - TimeUnit.DAYS.toSeconds(1), CATEGORY_SIZE, false);
            case WEEKLY -> ids = RoomVoteDao.getBestRoomsSince(now - TimeUnit.DAYS.toSeconds(7), CATEGORY_SIZE, false);
            case ALL_TIME -> ids = RoomVoteDao.getBestRoomsEver(CATEGORY_SIZE);
            default -> ids = new ArrayList<>();
        }

        List<Room> rooms = RoomDao.getRoomsByIds(ids);
        this.cache.put(ranking, new Ranking(rooms, System.currentTimeMillis()));
        return rooms;
    }

    /**
     * The week's best open rooms for "Anbefalede Rum" (not cached: the navigator asks for three).
     */
    public List<Room> getRecommended(int limit) {
        long since = DateUtil.getCurrentTimeSeconds() - TimeUnit.DAYS.toSeconds(7);
        return RoomDao.getRoomsByIds(RoomVoteDao.getBestRoomsSince(since, limit, true));
    }

    /**
     * Forget the cached rankings (after a vote).
     */
    public void reset() {
        this.cache.clear();
    }

    public static RoomRatings getInstance() {
        if (instance == null) {
            instance = new RoomRatings();
        }

        return instance;
    }
}
