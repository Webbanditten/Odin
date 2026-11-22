package org.alexdev.kepler.messages.outgoing.catalogue;

import org.alexdev.kepler.dao.mysql.FurniRevisionDao;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.Map;

/**
 * The furni revision list (hh_dynamic_downloader's handle_furni_revisions): two lists of class and revision, furni
 * first. The client keeps the highest revision per class and puts it into dynamic.download.name.template's %revision%
 * (a small cast "s_x" uses x's), so a furni republished from the CMS's furni editor gets a new URL. Classes without a
 * revision get an empty %revision%.
 */
public class SPRITE_LIST extends MessageComposer {
    private final Map<String, Integer> revisions;

    public SPRITE_LIST() {
        this.revisions = FurniRevisionDao.getRevisions();
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.revisions.size());

        for (var entry : this.revisions.entrySet()) {
            response.writeString(entry.getKey());
            response.writeInt(entry.getValue());
        }

        response.writeInt(0);
    }

    @Override
    public short getHeader() {
        return 295; // "Dg"
    }
}
