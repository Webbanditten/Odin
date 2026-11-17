package org.alexdev.kepler.messages.outgoing.purse;

import org.alexdev.kepler.game.catalogue.CatalogueItem;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VOUCHER_REDEEM_OK extends MessageComposer {
    private final List<CatalogueItem> redeemableItems;

    public VOUCHER_REDEEM_OK(List<CatalogueItem> redeemableItems) {
        this.redeemableItems = redeemableItems;
    }

    @Override
    public void compose(NettyResponse response) {
//        if tMsg.subject = 212 then
//        me.getInterface().hideVoucherWindow()
//        me.getInterface().setVoucherInput(1)
//        tConn = tMsg.connection
//        if tConn = void() then
//        return(1)
//        end if
//        tProductName = tConn.GetStrFrom()
//        if tProductName <> "" then
//                tResultStr = getText("purse_vouchers_furni_success") & "\r" & "\r"
//        repeat while tProductName <> ""
//        tDescription = tConn.GetStrFrom()
//        tResultStr = tResultStr & tProductName & "\r"
//        tProductName = tConn.GetStrFrom()
//        end repeat
//        return(executeMessage(#alert, [#Msg:tResultStr]))
//        else
//        return(executeMessage(#alert, [#Msg:"purse_vouchers_success"]))
//        end if
//      else

        if (this.redeemableItems != null && this.redeemableItems.size() > 0) {
            if (this.redeemableItems.size() <= 2) {
                for (CatalogueItem catalogueItem : this.redeemableItems) {
                    response.writeString(name(catalogueItem));
                    response.writeString(description(catalogueItem));
                }
            } else {
                response.writeString(this.redeemableItems.stream().map(VOUCHER_REDEEM_OK::name).collect(Collectors.joining(", " )));
                response.writeString("");
            }
        }
    }

    /** A package has no definition, it and special-sprite items (posters) have their own name; others the furni's. */
    private static String name(CatalogueItem item) {
        if ((item.isPackage() || item.getItemSpecialId() > 0) && item.getName() != null && !item.getName().isEmpty()) {
            return item.getName();
        }

        return item.getDefinition() != null ? item.getDefinition().getName() : item.getSaleCode();
    }

    private static String description(CatalogueItem item) {
        if ((item.isPackage() || item.getItemSpecialId() > 0) && item.getDescription() != null && !item.getDescription().isEmpty()) {
            return item.getDescription();
        }

        return item.getDefinition() != null ? item.getDefinition().getDescription() : "";
    }

    @Override
    public short getHeader() {
        return 212; // "CT"
    }
}