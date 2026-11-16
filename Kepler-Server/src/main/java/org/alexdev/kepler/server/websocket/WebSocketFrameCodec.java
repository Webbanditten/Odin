package org.alexdev.kepler.server.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;

/**
 * Turns WebSocket data frames into the byte stream the TCP decoders expect, and outgoing buffers into binary
 * frames. Frame boundaries don't matter: the game and MUS decoders reassemble messages from the stream as they
 * do for TCP. Ping, pong and close frames are handled by WebSocketServerProtocolHandler before this point.
 */
public class WebSocketFrameCodec extends MessageToMessageCodec<WebSocketFrame, ByteBuf> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        out.add(new BinaryWebSocketFrame(msg.retain()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) {
        if (frame instanceof BinaryWebSocketFrame || frame instanceof TextWebSocketFrame
                || frame instanceof ContinuationWebSocketFrame) {
            out.add(frame.content().retain());
        }
    }
}
