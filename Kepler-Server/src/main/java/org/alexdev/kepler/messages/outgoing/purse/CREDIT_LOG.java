package org.alexdev.kepler.messages.outgoing.purse;

import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.purse.CreditLog;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CREDIT_LOG extends MessageComposer {
    private final List<CreditLog> creditLog;

    public CREDIT_LOG(PlayerDetails details, List<CreditLog> creditLog) {
        this.creditLog = creditLog;
    }


    @Override
    public void compose(NettyResponse response) {
        StringBuilder logString = new StringBuilder();
        creditLog.forEach(log -> {
            Date date = new Date(log.getTimestamp()*1000);
            String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(date);
            String timeStr = new SimpleDateFormat("HH:mm:ss").format(date);
            // The 4th item is the purse's "BELØB" column (real money paid), shown only with purse.valuefield.active=1
            // (the purse layout that has room for a purse ad, Housekeeping > Ads); Odin has no such amount, so it's empty.
            logString.append(dateStr + "\t" + timeStr + "\t" + log.getCredits() + "\t\t\t" +  log.getType() + "\r");
        });
        response.writeString(logString);
    }

    @Override
    public short getHeader() {
        return 209; // "CQ"
    }
}