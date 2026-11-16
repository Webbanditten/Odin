package org.alexdev.kepler.server.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.server.mus.MusServer;
import org.alexdev.kepler.server.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * WebSocket front door for browser clients (DirPlayer). One port, two paths:
 * a path ending in {@code /game} speaks the game protocol, one ending in {@code /mus} speaks MUS.
 * After the handshake each connection gets the same handlers as a TCP connection, registered with the
 * game or MUS server, so bans, connection limits, HELLO and the message handlers behave exactly the same.
 */
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /** The client's address: the TCP peer, or the X-Forwarded-For client when the peer is a trusted proxy. */
    public static final AttributeKey<String> REAL_IP = AttributeKey.valueOf("WebSocketRealIp");

    private static final int BACK_LOG = 20;

    private final String ip;
    private final int port;
    private final WebSocketSettings settings;
    private final NettyServer gameServer;
    private final MusServer musServer;
    private final ServerBootstrap bootstrap = new ServerBootstrap();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public WebSocketServer(String ip, int port, WebSocketSettings settings, NettyServer gameServer, MusServer musServer) {
        this.ip = ip;
        this.port = port;
        this.settings = settings;
        this.gameServer = gameServer;
        this.musServer = musServer;
    }

    public void createSocket() {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        this.bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        this.workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(threads) : new NioEventLoopGroup(threads);

        this.bootstrap.group(this.bossGroup, this.workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new WebSocketChannelInitializer(this))
                .option(ChannelOption.SO_BACKLOG, BACK_LOG)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true));
    }

    public void bind() {
        this.bootstrap.bind(new InetSocketAddress(this.ip, this.port)).addListener(future -> {
            if (!future.isSuccess()) {
                Log.getErrorLogger().error("Failed to start WebSocket server on address: {}:{}", this.ip, this.port);
            } else {
                log.info("WebSocket server is listening on {}:{} (paths /game and /mus)", this.ip, this.port);
            }
        });
    }

    public void dispose() throws InterruptedException {
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully().sync();
        }

        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully().sync();
        }
    }

    public WebSocketSettings getSettings() {
        return this.settings;
    }

    public NettyServer getGameServer() {
        return this.gameServer;
    }

    public MusServer getMusServer() {
        return this.musServer;
    }

    /**
     * Settings from server.ini ([WebSocket] section).
     *
     * @param trustedProxies peers whose X-Forwarded-For header is believed (exact IP match)
     * @param allowedOrigins Origin header values that may connect; empty allows any origin
     * @param maxFrameSize   largest accepted WebSocket frame payload, in bytes
     */
    public record WebSocketSettings(Set<String> trustedProxies, Set<String> allowedOrigins, int maxFrameSize) {
    }
}
