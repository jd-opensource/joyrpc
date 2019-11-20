package io.joyrpc.transport.netty4.ss5;

import io.joyrpc.constants.Constants;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.netty4.Plugin;
import io.joyrpc.transport.netty4.binder.HandlerBinder;
import io.joyrpc.transport.netty4.channel.NettyChannel;
import io.joyrpc.transport.netty4.handler.ConnectionChannelHandler;
import io.joyrpc.transport.netty4.handler.IdleHeartbeatHandler;
import io.joyrpc.transport.netty4.transport.NettyClientTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.constants.Constants.*;

public class NettySs5ClientTransport extends NettyClientTransport {

    protected SocketAddress ss5Address;

    protected String ss5User;

    protected String ss5Password;


    /**
     * 构造函数
     *
     * @param url
     */
    public NettySs5ClientTransport(URL url) {
        super(url);
        this.ss5Address = new InetSocketAddress(url.getString(SS5_HOST), url.getInteger(SS5_PORT));
        this.ss5User = url.getString(SS5_USER);
        this.ss5Password = url.getString(SS5_PASSWORD);
    }


    @Override
    protected Bootstrap handler(Bootstrap bootstrap, Channel[] channels, SslContext sslContext) {
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                //及时发送 与 缓存发送
                channels[0] = new NettyChannel(ch);
                //设置payload
                channels[0].setAttribute(Channel.PAYLOAD, url.getPositiveInt(Constants.PAYLOAD));
                //添加业务线程池到channel
                if (bizThreadPool != null) {
                    channels[0].setAttribute(Channel.BIZ_THREAD_POOL, bizThreadPool);
                }
                //添加连接事件监听
                ch.pipeline().addLast("connection", new ConnectionChannelHandler(channels[0], publisher));
                //添加编解码和处理链
                channels[0].setAttribute(Channel.IS_SERVER, false);
                HandlerBinder binder = Plugin.HANDLER_BINDER.get(codec.binder());
                binder.bind(ch.pipeline(), codec, handlerChain, channels[0]);
                //若配置idle心跳策略，配置心跳handler
                if (heartbeatStrategy != null && heartbeatStrategy.getHeartbeatMode() == HeartbeatStrategy.HeartbeatMode.IDLE) {
                    ch.pipeline().addLast("idleState",
                            new IdleStateHandler(0, heartbeatStrategy.getInterval(), 0, TimeUnit.MILLISECONDS))
                            .addLast("idleHeartbeat", new IdleHeartbeatHandler());
                }

                if (sslContext != null) {
                    ch.pipeline().addFirst("ssl", sslContext.newHandler(ch.alloc()));
                }
                ch.pipeline().addFirst("ss5", new Socks5ProxyHandler(ss5Address, ss5User, ss5Password));
            }
        });
        return bootstrap;
    }
}
