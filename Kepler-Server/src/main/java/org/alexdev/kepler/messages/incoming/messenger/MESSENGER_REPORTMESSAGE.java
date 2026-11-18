package org.alexdev.kepler.messages.incoming.messenger;

import org.alexdev.kepler.dao.mysql.MessengerDao;
import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.game.messenger.Messenger;
import org.alexdev.kepler.game.messenger.MessengerMessage;
import org.alexdev.kepler.game.messenger.MessengerUser;
import org.alexdev.kepler.game.moderation.cfh.CallForHelpManager;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.messages.outgoing.messenger.REMOVE_FRIEND;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;
import org.alexdev.kepler.util.DateUtil;

import java.util.List;

/**
 * "Rapporter" on a console message (the messenger's report window): moderators get it as a call for help, and, as
 * the window says ("Personen bliver automatisk fjernet fra din venneliste"), the sender is removed from the
 * reporter's friends.
 */
public class MESSENGER_REPORTMESSAGE implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        int messageId = reader.readInt();
        MessengerMessage message = MessengerDao.getReceivedMessage(messageId, player.getDetails().getId());

        if (message == null || message.getFromId() <= 0) {
            return; // not a message this player received (campaign messages have no sender)
        }

        int senderId = message.getFromId();
        String senderName = PlayerDao.getName(senderId);
        String sent = DateUtil.getDateAsString(message.getTimeSet());

        CallForHelpManager.getInstance().submitReport(player, String.format(
                "Rapporteret konsolbesked fra %s (sendt %s): %s", senderName != null ? senderName : "#" + senderId, sent, message.getMessage()));

        MessengerDao.markMessageRead(messageId);

        // Remove the friendship both ways, as MESSENGER_REMOVEBUDDY does.
        if (player.getMessenger().hasFriend(senderId)) {
            MessengerUser friend = player.getMessenger().getFriend(senderId);
            MessengerUser meAsFriend = player.getMessenger().getMessengerUser();

            player.getMessenger().removeFriend(senderId);

            Messenger friendMessenger = PlayerManager.getInstance().getMessengerData(senderId);

            if (friendMessenger != null) {
                friendMessenger.removeFriend(meAsFriend.getUserId());
            }

            Player sender = PlayerManager.getInstance().getPlayerById(senderId);

            if (sender != null) {
                sender.send(new REMOVE_FRIEND(meAsFriend));
            }

            player.send(new REMOVE_FRIEND(List.of(friend)));
        }
    }
}
