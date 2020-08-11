package io.joyrpc.transport.netty4.transport;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.constants.Constants;
import io.joyrpc.event.AsyncResult;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.exception.SslException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelManager.Connector;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy.HeartbeatMode;
import io.joyrpc.transport.netty4.Plugin;
import io.joyrpc.transport.netty4.binder.HandlerBinder;
import io.joyrpc.transport.netty4.channel.NettyClientChannel;
import io.joyrpc.transport.netty4.handler.ConnectionChannelHandler;
import io.joyrpc.transport.netty4.handler.IdleHeartbeatHandler;
import io.joyrpc.transport.netty4.ssl.SslContextManager;
import io.joyrpc.transport.transport.AbstractClientTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.joyrpc.constants.Constants.*;

/**
 * @date: 2019/2/21
 */
public class NettyClientTransport extends AbstractClientTransport {
    /**
     * 构造函数
     *
     * @param url
     */
    public NettyClientTransport(URL url) {
        super(url);
    }

    @Override
    public Connector getConnector() {
        return this::connect;
    }

    /**
     * 创建channel
     *
     * @param consumer
     */
    protected void connect(final Consumer<AsyncResult<Channel>> consumer) {
        //consumer不会为空
        if (codec == null) {
            consumer.accept(new AsyncResult<>(error("codec can not be null!")));
        } else {
            final EventLoopGroup[] ioGroups = new EventLoopGroup[1];
            final Channel[] channels = new Channel[1];
            //当出现异常的时候关闭线程池
            Consumer<AsyncResult<Channel>> myConsumer = result -> {
                try {
                    if (!result.isSuccess()) {
                        Channel channel = result.getResult();
                        channel.close();
                    }
                } finally {
                    consumer.accept(result);
                }
            };
            try {
                ioGroups[0] = EventLoopGroupFactory.getClientGroup(url);
                //获取SSL上下文
                SslContext sslContext = SslContextManager.getClientSslContext(url);
                //TODO 考虑根据不同的参数，创建不同的连接
                Bootstrap bootstrap = configure(new Bootstrap(), ioGroups[0], channels, sslContext);
                // Bind and start to accept incoming connections.
                bootstrap.connect(url.getHost(), url.getPort()).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        myConsumer.accept(new AsyncResult<>(channels[0]));
                    } else {
                        myConsumer.accept(new AsyncResult<>(new NettyClientChannel(f.channel(), ioGroups[0]), error(f.cause())));
                    }
                });
            } catch (SslException e) {
                myConsumer.accept(new AsyncResult<>(new NettyClientChannel(null, ioGroups[0]), e));
            } catch (ConnectionException e) {
                myConsumer.accept(new AsyncResult<>(new NettyClientChannel(null, ioGroups[0]), e));
            } catch (Throwable e) {
                //捕获Throwable，防止netty报错
                myConsumer.accept(new AsyncResult<>(new NettyClientChannel(null, ioGroups[0]), error(e)));
            }
        }
    }

    /**
     * 配置
     *
     * @param bootstrap  bootstrap
     * @param ioGroup    线程池
     * @param channels   通道
     * @param sslContext ssl上下文
     */
    protected Bootstrap configure(final Bootstrap bootstrap,
                                  final EventLoopGroup ioGroup,
                                  final Channel[] channels,
                                  final SslContext sslContext) {
        //Unknown channel option 'SO_BACKLOG' for channel
        bootstrap.group(ioGroup).channel(Constants.isUseEpoll(url) ? EpollSocketChannel.class : NioSocketChannel.class).
                option(ChannelOption.CONNECT_TIMEOUT_MILLIS, url.getPositiveInt(Constants.CONNECT_TIMEOUT_OPTION)).
                //option(ChannelOption.SO_TIMEOUT, url.getPositiveInt(Constants.SO_TIMEOUT_OPTION)).
                        option(ChannelOption.TCP_NODELAY, url.getBoolean(TCP_NODELAY)).
                option(ChannelOption.SO_KEEPALIVE, url.getBoolean(Constants.SO_KEEPALIVE_OPTION)).
                option(ChannelOption.ALLOCATOR, BufAllocator.create(url)).
                option(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(
                                url.getPositiveInt(Constants.WRITE_BUFFER_LOW_WATERMARK_OPTION),
                                url.getPositiveInt(Constants.WRITE_BUFFER_HIGH_WATERMARK_OPTION))).
                option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT).
                handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        //及时发送 与 缓存发送
                        channels[0] = new NettyClientChannel(ch, ioGroup);
                        //设置
                        channels[0].
                                setAttribute(Channel.PAYLOAD, url.getPositiveInt(Constants.PAYLOAD)).
                                setAttribute(Channel.BIZ_THREAD_POOL, bizThreadPool, (k, v) -> v != null);
                        //添加连接事件监听
                        ch.pipeline().addLast("connection", new ConnectionChannelHandler(channels[0], publisher));
                        //添加编解码和处理链
                        HandlerBinder binder = Plugin.HANDLER_BINDER.get(codec.binder());
                        binder.bind(ch.pipeline(), codec, handlerChain, channels[0]);
                        //若配置idle心跳策略，配置心跳handler
                        if (heartbeatStrategy != null && heartbeatStrategy.getHeartbeatMode() == HeartbeatMode.IDLE) {
                            ch.pipeline().
                                    addLast("idleState", new IdleStateHandler(0, heartbeatStrategy.getInterval(), 0, TimeUnit.MILLISECONDS)).
                                    addLast("idleHeartbeat", new IdleHeartbeatHandler());
                        }
                        if (sslContext != null) {
                            ch.pipeline().addFirst("ssl", sslContext.newHandler(ch.alloc()));
                        }
                        //若开启了ss5代理，添加ss5
                        if (url.getBoolean(SS5_ENABLE)) {
                            String host = url.getString(SS5_HOST);
                            if (host != null && !host.isEmpty()) {
                                InetSocketAddress ss5Address = new InetSocketAddress(host, url.getInteger(SS5_PORT));
                                ch.pipeline().addFirst("ss5", new Socks5ProxyHandler(ss5Address, url.getString(SS5_USER), url.getString(SS5_PASSWORD)));
                            }
                        }
                    }
                });
        return bootstrap;
    }

    /**
     * 连接异常
     *
     * @param message
     * @return
     */
    protected Throwable error(final String message) {
        return message == null || message.isEmpty() ? new ConnectionException("Unknown error.") : new ConnectionException(message);
    }

    /**
     * 连接异常
     *
     * @param throwable
     * @return
     */
    protected Throwable error(final Throwable throwable) {
        return throwable == null ?
                new ConnectionException("Unknown error.") :
                new ConnectionException(throwable.getMessage(), throwable);
    }

}
