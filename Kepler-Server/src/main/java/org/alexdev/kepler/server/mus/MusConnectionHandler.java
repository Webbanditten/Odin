package org.alexdev.kepler.server.mus;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.alexdev.kepler.Kepler;
import org.alexdev.kepler.dao.mysql.CurrencyDao;
import org.alexdev.kepler.dao.mysql.ItemDao;
import org.alexdev.kepler.dao.mysql.PhotoDao;
import org.alexdev.kepler.dao.mysql.PlayerDao;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.ItemManager;
import org.alexdev.kepler.game.item.Photo;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.player.PlayerDetails;
import org.alexdev.kepler.game.player.PlayerManager;
import org.alexdev.kepler.log.Log;
import org.alexdev.kepler.messages.outgoing.user.currencies.FILM;
import org.alexdev.kepler.server.mus.connection.MusClient;
import org.alexdev.kepler.server.mus.streams.MusMessage;
import org.alexdev.kepler.server.mus.streams.MusPropList;
import org.alexdev.kepler.server.mus.streams.MusTypes;
import org.alexdev.kepler.server.netty.NettyPlayerNetwork;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.StringUtil;
import org.alexdev.kepler.util.config.ServerConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MusConnectionHandler extends SimpleChannelInboundHandler<MusMessage> {
    final private static AttributeKey<MusClient> MUS_CLIENT_KEY = AttributeKey.valueOf("MusClient");
    final private static Logger log = LoggerFactory.getLogger(MusConnectionHandler.class);

    private final MusServer server;

    public MusConnectionHandler(MusServer musServer) {
        this.server = musServer;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        if (!this.server.getChannels().add(ctx.channel()) || Kepler.isShuttingdown()) {
            Log.getErrorLogger().error("Could not accept MUS connection from {}", ctx.channel().remoteAddress().toString().replace("/", "").split(":")[0]);
            ctx.close();
            return;
        }

        MusClient client = new MusClient(ctx.channel());
        ctx.channel().attr(MUS_CLIENT_KEY).set(client);

        //log.info("[MUS] Connection from {}", ctx.channel().remoteAddress().toString().replace("/", "").split(":")[0]);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        this.server.getConnectionIds().getAndDecrement(); // Decrement because we don't want it to reach Integer.MAX_VALUE
        this.server.getChannels().remove(ctx.channel());

        //log.info("[MUS] Disconnection from {}", ctx.channel().remoteAddress().toString().replace("/", "").split(":")[0]);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, MusMessage message) {
        MusMessage reply;
        MusClient client = ctx.channel().attr(MUS_CLIENT_KEY).get();

        if (client == null) {
            ctx.close();
            return;
        }

        try {
            //log.info("[MUS] Message from {}: {}", ctx.channel().remoteAddress().toString().replace("/", "").split(":")[0], message.toString());

            if (message.getSubject().equals("Logon")) {
                reply = new MusMessage();
                reply.setSubject("Logon");
                reply.setContentType(MusTypes.String);
                reply.setContentString("Kepler: Habbo Hotel shockwave emulator");
                ctx.channel().writeAndFlush(reply);

                reply = new MusMessage();
                reply.setSubject("HELLO");
                reply.setContentType(MusTypes.String);
                reply.setContentString("");
                ctx.channel().writeAndFlush(reply);
            }

            if (message.getSubject().equals("LOGIN")) {
                String[] credentials = message.getContentString().split(" ", 2);

                Player player = null;
                int userId = -1;

                if (!StringUtils.isNumeric(credentials[0])) {
                    String username = credentials[0];
                    String password = credentials[1];

                    PlayerDetails playerDetails = new PlayerDetails();

                    if (PlayerDao.login(playerDetails, username, password)) {
                        player =  PlayerManager.getInstance().getPlayerById(playerDetails.getId());
                        userId = playerDetails.getId();
                    } else {
                        player = null;
                    }
                } else {
                    userId = Integer.valueOf(credentials[0]);
                    player = PlayerManager.getInstance().getPlayerById(userId);
                }

                // Er, ma, gerd, we logged in! ;O
                if (player != null && NettyPlayerNetwork.getIpAddress(player.getNetwork().getChannel()).equals(NettyPlayerNetwork.getIpAddress(ctx.channel()))) {
                    //System.out.println("RCON user " + player.getDetails().getName() + " logged in");
                    client.setUserId(userId);
                } else {
                    log.info("[MUS] RCON user kicked due to inappropriate formed message {}", ctx.channel().remoteAddress().toString().replace("/", ""));
                    ctx.channel().close(); // Lol, bye, imposter scum!
                }
            }

            if (message.getSubject().equals("PHOTOTXT")) {
                if (client.getUserId() > 0) {
                    client.setPhotoText(StringUtil.filterInput(message.getContentString().substring(1), true));
                }
            }

            if (message.getSubject().equals("BINDATA")) {
                Player player = PlayerManager.getInstance().getPlayerById(client.getUserId());

                if (player == null) {
                    return;
                }

                if (client.getUserId() < 1) {
                    return;
                }

                if (player.getRoomUser().getRoom() == null) {
                    return;
                }

                long timeSeconds = DateUtil.getCurrentTimeSeconds();

                String time = message.getContentPropList().getPropAsString("time");
                Integer cs = message.getContentPropList().getPropAsInt("cs");
                byte[] image = message.getContentPropList().getPropAsBytes("image");
                String photoText = client.getPhotoText();

                Item photo = new Item();
                photo.setOwnerId(client.getUserId());
                photo.setDefinitionId(ItemManager.getInstance().getDefinitionBySprite("photo").getId());
                photo.setCustomData(DateUtil.getDateAsString(timeSeconds) + "\r" + photoText);
                photo.setOwnedSince(DateUtil.getCurrentTimeSeconds());
                ItemDao.newItem(photo);

                PhotoDao.addPhoto(photo.getId(), client.getUserId(), DateUtil.getCurrentTimeSeconds(), image, cs);

                reply = new MusMessage();
                reply.setSubject("BINDATA_SAVED");
                reply.setContentType(MusTypes.String);
                reply.setContentString(Integer.toString(client.getUserId()));
                ctx.channel().writeAndFlush(reply);

                player.getInventory().addItem(photo);
                player.getInventory().getView("new");

                CurrencyDao.decreaseFilm(player.getDetails(), 1);
                player.send(new FILM(player.getDetails()));
            }

            if (message.getSubject().equals("GETBINDATA")) {
                int photoID = Integer.parseInt(message.getContentString().split(" ")[0]);

                Photo photo = PhotoDao.getPhoto(photoID);

                if (photo == null) {
                    return;
                }

                if (client.getUserId() < 1) {
                    return;
                }

                reply = new MusMessage();
                reply.setSubject("BINARYDATA");
                reply.setContentType(MusTypes.PropList);
                reply.setContentPropList(new MusPropList(3));
                reply.getContentPropList().setPropAsBytes("image", MusTypes.Media, photo.getData());
                reply.getContentPropList().setPropAsString("time", DateUtil.getDateAsString(photo.getTime()));
                reply.getContentPropList().setPropAsInt("cs", photo.getChecksum());
                ctx.channel().writeAndFlush(reply);
            }

        } catch (Exception ex) {
            Log.getErrorLogger().error("Exception occurred when handling MUS message: ", ex);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof Exception) {
            if (!(cause instanceof IOException)) {
                Log.getErrorLogger().error("[MUS] Netty error occurred: ", cause);
            }
        }

        ctx.close();
    }
}
