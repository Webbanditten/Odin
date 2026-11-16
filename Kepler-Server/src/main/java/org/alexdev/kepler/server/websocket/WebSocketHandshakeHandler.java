package org.alexdev.kepler.server.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

/**
 * Looks at the upgrade request before {@code WebSocketServerProtocolHandler} answers it: picks the route from
 * the path, checks the Origin, and works out the client's address. Anything that isn't a WebSocket upgrade to a
 * known route gets a plain HTTP error and is closed.
 */
public class WebSocketHandshakeHandler extends ChannelInboundHandlerAdapter {
    public static final AttributeKey<Route> ROUTE = AttributeKey.valueOf("WebSocketRoute");

    /** An IPv4 or IPv6 literal, loosely. Keeps header junk out of logs and ban lookups. */
    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9A-Fa-f:.]{2,45}$");

    public enum Route {
        GAME, MUS;

        /** Matches on the last path segment, so a reverse proxy can mount the endpoint under any prefix. */
        static Route fromPath(String path) {
            String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

            if (p.endsWith("/game")) {
                return GAME;
            }

            if (p.endsWith("/mus")) {
                return MUS;
            }

            return null;
        }
    }

    private final WebSocketServer.WebSocketSettings settings;

    public WebSocketHandshakeHandler(WebSocketServer.WebSocketSettings settings) {
        this.settings = settings;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof FullHttpRequest request)) {
            ctx.fireChannelRead(msg);
            return;
        }

        Route route = Route.fromPath(new QueryStringDecoder(request.uri()).path());

        if (route == null) {
            reject(ctx, request, HttpResponseStatus.NOT_FOUND);
            return;
        }

        if (!request.headers().containsValue(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true)) {
            reject(ctx, request, HttpResponseStatus.UPGRADE_REQUIRED);
            return;
        }

        if (!this.settings.allowedOrigins().isEmpty()) {
            String origin = request.headers().get(HttpHeaderNames.ORIGIN);

            if (origin == null || !this.settings.allowedOrigins().contains(origin)) {
                reject(ctx, request, HttpResponseStatus.FORBIDDEN);
                return;
            }
        }

        ctx.channel().attr(ROUTE).set(route);
        ctx.channel().attr(WebSocketServer.REAL_IP).set(clientAddress(ctx, request));
        ctx.pipeline().remove(this);
        ctx.fireChannelRead(request);
    }

    /**
     * The TCP peer, unless the peer is a trusted proxy: then the right-most X-Forwarded-For entry that isn't
     * itself a trusted proxy (X-Real-IP if there is no X-Forwarded-For).
     */
    private String clientAddress(ChannelHandlerContext ctx, FullHttpRequest request) {
        String peer = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        if (!this.settings.trustedProxies().contains(peer)) {
            return peer;
        }

        String forwardedFor = request.headers().get("X-Forwarded-For");

        if (forwardedFor != null) {
            String[] hops = forwardedFor.split(",");

            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();

                if (!IP_LITERAL.matcher(hop).matches()) {
                    break;
                }

                if (!this.settings.trustedProxies().contains(hop)) {
                    return hop;
                }
            }
        }

        String realIp = request.headers().get("X-Real-IP");

        if (realIp != null && IP_LITERAL.matcher(realIp.trim()).matches()) {
            return realIp.trim();
        }

        return peer;
    }

    private static void reject(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), status,
                Unpooled.copiedBuffer(status.toString() + "\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, response.content().readableBytes());
        ReferenceCountUtil.release(request);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
