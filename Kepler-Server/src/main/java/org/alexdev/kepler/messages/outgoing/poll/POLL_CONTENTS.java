package org.alexdev.kepler.messages.outgoing.poll;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;
import org.alexdev.kepler.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class POLL_CONTENTS extends MessageComposer {

    @Override
    public void compose(NettyResponse response) {
        // tPollID
        response.writeInt(1);
        // tPollHeadLine
        response.writeString("TEST AFSTEMNING");
        // tPollThankYou
        response.writeString("Tak fordi du deltog i afstemningen!");

        // Question count
        response.writeInt(2);


        // questionID
        response.writeInt(1);
        // questionNumber
        response.writeInt(1);
        // questionType
        response.writeInt(2);
        // questionText
        response.writeString(StringUtil.charsetEncode("æøå"));
        // tSelectionCount
        response.writeInt(2);
        // minSelect
        response.writeInt(1);
        // maxSelect
        response.writeInt(1);
        // questionText
        response.writeString("Nej");
        response.writeString("Ja");






        // questionID
        response.writeInt(2);
        // questionNumber
        response.writeInt(2);
        // questionType
        response.writeInt(1);
        // questionText
        response.writeString(StringUtil.charsetEncode("Vælg dine favorit mærker"));
        // tSelectionCount
        response.writeInt(3);
        // minSelect
        response.writeInt(1);
        // maxSelect
        response.writeInt(2);
        // questionText
        response.writeString("Nokia");
        response.writeString("Sony");
        response.writeString("Apple");

    }

    @Override
    public short getHeader() {
        return 317;
    }
}