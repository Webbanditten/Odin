package org.alexdev.kepler.game.commandqueue.commands;

import org.alexdev.kepler.dao.mysql.ClubPeriodDao;
import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.game.club.ClubSubscription;
import org.alexdev.kepler.game.commandqueue.CommandTemplate;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.util.DateUtil;

/**
 * Housekeeping's Habbo Club admin (OdinCMS, the user page): Days &gt; 0 gives that many days of Habbo Club as a purchase
 * would, without charging credits (online: ClubSubscription.subscribeClub, so the welcome gift, badge and fuserights
 * follow at once); Days &lt;= 0 ends the membership now. Works for players who aren't online too.
 */
public class ClubDaysCommand implements Command {
    @Override
    public void executeCommand(CommandTemplate commandArgs) {
        Player player = PlayerManager.getInstance().getPlayerById(commandArgs.UserId);

        try {
            if (commandArgs.Days > 0 && player != null) {
                ClubSubscription.subscribeClub(player, commandArgs.Days, 0);
                return;
            }

            PlayerDetails details = player != null ? player.getDetails() : PlayerDao.getDetails(commandArgs.UserId);

            if (details == null) {
                return;
            }

            long now = DateUtil.getCurrentTimeSeconds();

            if (commandArgs.Days > 0) {
                // As subscribeClub, for a player who isn't online: the gifts follow at their next login.
                if (details.getFirstClubSubscription() == 0) {
                    details.setFirstClubSubscription(now);
                }

                long secondsToAdd = 24L * 60 * 60 * commandArgs.Days;
                details.setClubExpiration(details.getClubExpiration() - now <= 0 ? now + secondsToAdd + 1 : details.getClubExpiration() + secondsToAdd);
                ClubPeriodDao.syncOpenPeriod(details.getId(), now, details.getClubExpiration());
            } else {
                if (details.getClubExpiration() <= now) {
                    return;
                }

                details.setClubExpiration(now);
                ClubPeriodDao.endOpenPeriod(details.getId(), now);
            }

            PlayerDao.saveSubscription(details);

            if (player != null) {
                player.refreshClub();
                player.refreshFuserights();
            }
        } catch (Exception e) {
            Log.getErrorLogger().error("club_days for user " + commandArgs.UserId + " failed", e);
        }
    }
}
