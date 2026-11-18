package org.alexdev.kepler.game.club;

import org.alexdev.kepler.dao.mysql.BadgeDao;
import org.alexdev.kepler.dao.mysql.ClubGiftDao;
import org.alexdev.kepler.dao.mysql.ClubPeriodDao;
import org.alexdev.kepler.dao.mysql.PurchaseLogDao;
import org.alexdev.kepler.dao.mysql.CurrencyDao;
import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.dao.mysql.PurseDao;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.ItemManager;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.messages.outgoing.rooms.badges.AVAILABLE_BADGES;
import org.alexdev.kepler.messages.outgoing.user.CLUB_INFO;
import org.alexdev.kepler.messages.outgoing.user.currencies.CREDIT_BALANCE;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.config.GameConfiguration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public class ClubSubscription {
    private static String[] giftOrder = new String[]{
            "club_sofa",
            "hcplastos",
            "mocchamaster",
            "edicehc",
            "hcamme",
            "doorD",
            "hcsohva",
            "hc_lmp",
            "hc_tbl",
            "hc_chr",
            "hc_dsk",
            "hc_tv",
            "hc_crtn",
            "hc_crpt",
            "hc_wall_lamp",
            "deal_hcrollers",
            "hc_bkshlf",
            "hc_trll",
            "hc_machine",
            "hc_rntgn",
            "hc_djset",
            "hc_lmpst",
            "hc_frplc",
            "hc_btlr"
    };

    /**
     * A Habbo Club month ("period" in the client): 31 days, like the purchase options.
     */
    public static final long MONTH_SECONDS = 31L * 24 * 60 * 60;

    /**
     * Months as member for the gold HC badge (HC2): from the first day of the 13th month, as on Habbo.
     */
    public static final int GOLD_MONTHS = 12;

    /**
     * Membership time up to now, in seconds: the time inside every membership period (users_club_periods), so breaks
     * in the subscription don't count.
     */
    public static long getMemberSeconds(PlayerDetails details) {
        return ClubPeriodDao.getMemberSeconds(details.getId(), DateUtil.getCurrentTimeSeconds());
    }

    /**
     * Completed months as member, breaks excluded.
     */
    public static int getMemberMonths(PlayerDetails details) {
        return (int) (getMemberSeconds(details) / MONTH_SECONDS);
    }

    /**
     * Send the club status: days left in this month, months as member, prepaid months.
     *
     * @param player the player to send it to
     */
    public static void sendHcDays(Player player) {
        long now = DateUtil.getCurrentTimeSeconds();

        int totalDays = 0;
        int remainingDaysThisMonth = 0;
        int prepaidMonths = 0;

        if (player.getDetails().getClubExpiration() > now) {
            // At least 1 while subscribed: with 0 days left the client shows the "membership has ended" window.
            totalDays = Math.max(1, (int) ((player.getDetails().getClubExpiration() - now) / 60 / 60 / 24));
            remainingDaysThisMonth = ((totalDays - 1) % 31) + 1;
            prepaidMonths = (totalDays - remainingDaysThisMonth) / 31;
        }

        // Also for a former member: the client then shows "your membership has ended" with these months instead of
        // the page for first-time buyers, and thanks a returning member as one.
        int sinceMonths = getMemberMonths(player.getDetails());

        player.send(new CLUB_INFO(remainingDaysThisMonth, sinceMonths, prepaidMonths));
    }

    /**
     * Subscribe to Habbo club with credits and days indicated, if 0 days, the function will not proceed.
     * If the credits amount is 0 or less then no credits will be charged.
     *
     * @param player the player to subscribe to club
     * @param days the amount of days to subscribe for
     * @param credits the amount of credits cost
     */
    public static void subscribeClub(Player player, int days, int credits) throws Exception {
        if (days <= 0) {
            return;
        }

        if (player.getDetails().getCredits() < credits) {
            return;
        }

        long now = DateUtil.getCurrentTimeSeconds();

        long daysInSeconds = 24 * 60 * 60;
        long secondsToAdd = (daysInSeconds * days);

        if (player.getDetails().getFirstClubSubscription() == 0) {
            player.getDetails().setFirstClubSubscription(now);
        }

        if (player.getDetails().getClubExpiration() - now <= 0) {
            player.getDetails().setClubExpiration(now + secondsToAdd + 1);
        } else {
            player.getDetails().setClubExpiration(player.getDetails().getClubExpiration() + secondsToAdd);
        }

        // Extends the running membership period, or starts a new one after a break.
        ClubPeriodDao.syncOpenPeriod(player.getDetails().getId(), now, player.getDetails().getClubExpiration());
        PlayerDao.saveSubscription(player.getDetails());

        player.refreshClub();
        player.refreshFuserights();

        if (credits > 0) {
            PurseDao.logCreditSpend("club_habbo", player.getDetails(), -credits);
            PurchaseLogDao.log(player.getDetails(), PurchaseLogDao.CLUB, null, null, null, "Habbo Club (" + days + " dage)", credits, null);
            CurrencyDao.decreaseCredits(player.getDetails(), credits);
            player.send(new CREDIT_BALANCE(player.getDetails()));
        }

        // A first-time member gets the welcome gift (gift 0, club_sofa) straight away.
        var progress = ClubGiftDao.getGiftProgress(player.getDetails().getId());

        if (progress != null && progress.getLeft() == 0) {
            giveGifts(player, 1);
        }
    }

    /**
     * How many gifts are due: one for every gift interval (club.gift.interval + club.gift.timeunit) of membership not
     * yet paid out, plus the welcome gift for a member who never got one. Only while subscribed; what a member earned
     * before a break waits for them.
     */
    public static int getGiftsDue(PlayerDetails details) {
        if (!details.hasClubSubscription()) {
            return 0;
        }

        var progress = ClubGiftDao.getGiftProgress(details.getId());

        if (progress == null) {
            return 0;
        }

        long interval = getClubGiftSeconds();
        long unpaid = Math.max(0, getMemberSeconds(details) - progress.getRight());
        long due = interval > 0 ? unpaid / interval : 0;

        if (progress.getLeft() == 0) {
            due++;
        }

        return (int) Math.min(due, Integer.MAX_VALUE);
    }

    public static boolean isGiftDue(Player player) {
        return getGiftsDue(player.getDetails()) > 0;
    }

    /**
     * Hand out up to {@code count} of the gifts that are due. Gift n (n = gifts received so far) is giftOrder[n], so
     * every member goes through the order one gift at a time, however their membership was split up.
     *
     * @return the gifts handed out
     */
    public static int giveGifts(Player player, int count) throws Exception {
        int toGive = Math.min(count, getGiftsDue(player.getDetails()));
        long interval = getClubGiftSeconds();
        String label = GameConfiguration.getInstance().getString("club.gift.present.label");
        int given = 0;

        while (given < toGive) {
            var progress = ClubGiftDao.getGiftProgress(player.getDetails().getId());

            if (progress == null) {
                break;
            }

            int claimed = progress.getLeft();
            String sprite = giftOrder[claimed % giftOrder.length];

            // The welcome gift isn't paid for by membership time; the others pay out one interval each.
            if (!ClubGiftDao.claimGift(player.getDetails().getId(), claimed, claimed == 0 ? 0 : interval)) {
                break;
            }

            ClubGiftDao.addGift(player.getDetails().getId(), sprite);
            Item item = ItemManager.getInstance().createGift(player.getDetails(), player.getDetails(), sprite, label, "");
            player.getInventory().addItem(item);
            given++;
        }

        if (given > 0) {
            player.getInventory().getView("new");
        }

        return given;
    }

    /**
     * Get the choice data for HC.
     *
     * @param choice the choice, 1, 2 or 3
     * @return the pair, days/credits
     */
    public static Pair<Integer, Integer> getChoiceData(int choice) {
        int days = -1;
        int credits = -1;

        switch (choice) {
            case 1:
            {
                credits = 25;
                days = 31;
                break;
            }
            case 2:
            {
                credits = 60;
                days = 93;
                break;
            }
            case 3:
            {
                credits = 105;
                days = 186;
                break;
            }
        }

        return Pair.of(credits, days);
    }

    /**
     * Get the offset of seconds required until the next gift is allowed.
     *
     * @return the offset seconds
     */
    private static long getClubGiftSeconds() {
        return TimeUnit.valueOf(GameConfiguration.getInstance().getString("club.gift.timeunit")).toSeconds(GameConfiguration.getInstance().getInteger("club.gift.interval"));
    }

    /**
     * Refresh the HC badges: HC1 while subscribed, HC2 (gold) from 12 months as member. Both are given in memory on every
     * login, not stored.
     *
     * @param player the player to check against
     */
    public static void refreshBadge(Player player) {
        var details = player.getDetails();

        if (!details.hasClubSubscription()) {
            // If the database still thinks we have Habbo club even after it expired, reset it back to 0.
            // The membership history stays in users_club_periods.
            if (details.getClubExpiration() > 0) {
                details.setClubExpiration(0);
                player.refreshFuserights();
                PlayerDao.saveSubscription(details);
            }

            details.getBadges().remove("HC1"); // If their HC ran out, remove badge.
            details.getBadges().remove("HC2"); // No gold badge when not subscribed.
        } else {
            // Club granted outside the game (housekeeping, the database) gets its membership period here.
            ClubPeriodDao.syncOpenPeriod(details.getId(), DateUtil.getCurrentTimeSeconds(), details.getClubExpiration());

            if (!details.getBadges().contains("HC1")) {
                details.getBadges().add("HC1");
            }

            if (getMemberMonths(details) >= GOLD_MONTHS) {
                if (!details.getBadges().contains("HC2")) {
                    details.getBadges().add("HC2");
                }
            } else if (!BadgeDao.hasBadge(details.getId(), "HC2")) {
                details.getBadges().remove("HC2"); // Given by the old rule (time since the first subscription).
            }
        }

        // A club badge that's gone can't stay on show: HC1 instead of HC2 while subscribed, otherwise none.
        String current = details.getCurrentBadge();

        if (("HC1".equals(current) || "HC2".equals(current)) && !details.getBadges().contains(current)) {
            details.setCurrentBadge(details.getBadges().contains("HC1") ? "HC1" : "");
            BadgeDao.saveCurrentBadge(details);
        }

        player.send(new AVAILABLE_BADGES(details));
    }
}
