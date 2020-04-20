package org.alexdev.kepler.messages.outgoing.rooms.user;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;
import org.alexdev.kepler.util.encoding.VL64Encoding;

public class QUEUETOROOM extends MessageComposer {
    @Override
    public void compose(NettyResponse response) {
        /*
        tSetCount: int
        tQueueData: [{
            name: string
            target: int
            data: tQueueCollection: [{
                tQueueID: string
                tQueueLength: int
            }]


  tSetCount = tConn.GetIntFrom()

    tQueueSetName = tConn.GetStrFrom()
    tQueueTarget = tConn.GetIntFrom()
    tNumberOfQueues = tConn.GetIntFrom()
      tQueueID = tConn.GetStrFrom()
      tQueueLength = tConn.GetIntFrom()


        }]
        * */

        // (tSetCount) Amount of sets (queues)
        response.writeInt(2);


        // Queue 1

        // (tQueueSetName)Name on target = fx c, dc
        response.writeString("dc");

        // (tQueueTarget) target?
        response.writeInt(1);

        // (tNumberOfQueues) Amount of queues (HC=c and non HC=d)
        response.writeInt(2);

            // (tQueueID)
            response.writeString("d");
            // (tQueueLength)
            response.writeInt(2);

            // (tQueueID)
            response.writeString("c");
            // (tQueueLength)
            response.writeInt(2);


        // Queue 2

        // (tQueueSetName)
        response.writeString("e2");

        // (tQueueTarget) target?
        response.writeInt(2);

        // (tNumberOfQueues)
        response.writeInt(1);

            // (tQueueID)
            response.writeString("e2");
            // (tQueueLength)
            response.writeInt(6);

        // Queue 3

        // (tQueueSetName)
        response.writeString("other");

        // (tQueueTarget) target?
        response.writeInt(1);

            // (tNumberOfQueues)
            response.writeInt(1);

            // (tQueueID)
            response.writeString("e2");
            // (tQueueLength)
            response.writeInt(6);

    }

    @Override
    public short getHeader() {
        return 259;
    }
}
