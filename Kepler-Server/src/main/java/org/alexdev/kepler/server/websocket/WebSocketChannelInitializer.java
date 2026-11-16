package org.alexdev.kepler.server.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * Pipeline until the WebSocket handshake is done. {@link WebSocketRouteHandler} then adds the game or MUS
 * handlers behind the frame codec.
 */
public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    /** Seconds a connection may take to send its upgrade request. */
    private static final int HANDSHAKE_TIMEOUT_SECONDS = 15;

    private final WebSocketServer server;

    public WebSocketChannelInitializer(WebSocketServer server) {
        this.server = server;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                .websocketPath("/")
                .checkStartsWith(true)                // any path; WebSocketHandshakeHandler picks the route
                .maxFramePayloadLength(this.server.getSettings().maxFrameSize())
                .allowExtensions(false)
                .handleCloseFrames(true)
                .dropPongFrames(true)
                .handshakeTimeoutMillis(HANDSHAKE_TIMEOUT_SECONDS * 1000L)
                .build();

        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("handshakeTimeout", new ReadTimeoutHandler(HANDSHAKE_TIMEOUT_SECONDS));
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(8192));
        pipeline.addLast("wsHandshake", new WebSocketHandshakeHandler(this.server.getSettings()));
        pipeline.addLast("wsProtocol", new WebSocketServerProtocolHandler(config));
        pipeline.addLast("wsRoute", new WebSocketRouteHandler(this.server));
    }
}
