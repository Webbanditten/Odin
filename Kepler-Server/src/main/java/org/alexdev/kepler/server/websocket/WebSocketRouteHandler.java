package org.alexdev.kepler.server.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.alexdev.kepler.server.mus.MusConnectionHandler;
import org.alexdev.kepler.server.mus.codec.MusNetworkDecoder;
import org.alexdev.kepler.server.mus.codec.MusNetworkEncoder;
import org.alexdev.kepler.server.netty.codec.NetworkDecoder;
import org.alexdev.kepler.server.netty.codec.NetworkEncoder;
import org.alexdev.kepler.server.netty.connections.ConnectionHandler;
import org.alexdev.kepler.server.netty.connections.IdleConnectionHandler;

/**
 * When the handshake completes, installs the same handlers a TCP connection gets (see
 * NettyChannelInitializer and MusChannelInitializer) behind a frame codec, then sends them the
 * channelRegistered event they missed. That is where ConnectionHandler checks bans and connection limits and
 * sends HELLO, and where MusConnectionHandler registers the client.
 */
public class WebSocketRouteHandler extends ChannelInboundHandlerAdapter {
    private final WebSocketServer server;

    public WebSocketRouteHandler(WebSocketServer server) {
        this.server = server;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete)) {
            super.userEventTriggered(ctx, evt);
            return;
        }

        ChannelPipeline pipeline = ctx.pipeline();

        if (pipeline.get("handshakeTimeout") != null) {
            pipeline.remove("handshakeTimeout");
        }

        pipeline.addAfter(ctx.name(), "wsFrames", new WebSocketFrameCodec());

        if (ctx.channel().attr(WebSocketHandshakeHandler.ROUTE).get() == WebSocketHandshakeHandler.Route.MUS) {
            pipeline.addAfter("wsFrames", "frameDecoder", new LengthFieldBasedFrameDecoder(1683226630, 2, 4, 0, 0));
            pipeline.addAfter("frameDecoder", "gameDecoder", new MusNetworkDecoder());
            pipeline.addAfter("gameDecoder", "gameEncoder", new MusNetworkEncoder());
            pipeline.addAfter("gameEncoder", "handler", new MusConnectionHandler(this.server.getMusServer()));
        } else {
            pipeline.addAfter("wsFrames", "gameEncoder", new NetworkEncoder());
            pipeline.addAfter("gameEncoder", "gameDecoder", new NetworkDecoder());
            pipeline.addAfter("gameDecoder", "handler", new ConnectionHandler(this.server.getGameServer()));
            pipeline.addAfter("handler", "idleStateHandler", new IdleStateHandler(60, 0, 0));
            pipeline.addAfter("idleStateHandler", "idleHandler", new IdleConnectionHandler());
        }

        // Only the handlers after this one see the event; the HTTP/WebSocket ones already had it.
        ctx.fireChannelRegistered();
        pipeline.remove(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Before the handshake (bad request, handshake timeout, reset): nothing to clean up, just close.
        ctx.close();
    }
}
