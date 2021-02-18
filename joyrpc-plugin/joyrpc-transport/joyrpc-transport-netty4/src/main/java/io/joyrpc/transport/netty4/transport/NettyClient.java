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
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.exception.SslException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelManager.Connector;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy.HeartbeatMode;
import io.joyrpc.transport.netty4.pipeline.PipelineFactory;
import io.joyrpc.transport.netty4.channel.NettyClientChannel;
import io.joyrpc.transport.netty4.handler.ConnectionHandler;
import io.joyrpc.transport.netty4.handler.IdleHeartbeatHandler;
import io.joyrpc.transport.netty4.ssl.SslContextManager;
import io.joyrpc.transport.AbstractClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.transport.netty4.Plugin.PIPELINE_FACTORY;

/**
 * Netty客户端连接
 */
public class NettyClient extends AbstractClient {
    /**
     * 构造函数
     *
     * @param url
     */
    public NettyClient(URL url) {
        super(url);
    }

    @Override
    public Connector getConnector() {
        return this::connect;
    }

    /**
     * 创建channel
     */
    protected CompletableFuture<Channel> connect() {
        CompletableFuture<Channel> future = new CompletableFuture<>();
        //consumer不会为空
        if (codec == null) {
            future.completeExceptionally(error("codec can not be null!"));
        } else {
            final EventLoopGroup[] ioGroups = new EventLoopGroup[1];
            final Channel[] channels = new Channel[1];
            try {
                ioGroups[0] = EventLoopGroupFactory.getClientGroup(url);
                //获取SSL上下文
                SslContext sslContext = SslContextManager.getClientSslContext(url);
                //TODO 考虑根据不同的参数，创建不同的连接
                Bootstrap bootstrap = configure(new Bootstrap(), ioGroups[0], channels, sslContext);
                // Bind and start to accept incoming connections.
                bootstrap.connect(url.getHost(), url.getPort()).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        future.complete(channels[0]);
                    } else {
                        future.completeExceptionally(error(f.cause()));
                    }
                });
            } catch (SslException e) {
                future.completeExceptionally(e);
            } catch (ConnectionException e) {
                future.completeExceptionally(e);
            } catch (Throwable e) {
                //捕获Throwable，防止netty报错
                future.completeExceptionally(error(e));
            }
        }
        return future;
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
                        ch.pipeline().addLast("connection", new ConnectionHandler(channels[0], publisher));
                        //添加编解码和处理链
                        PipelineFactory factory = PIPELINE_FACTORY.get(codec.pipeline());
                        factory.build(ch.pipeline(), codec, chain, channels[0]);
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
     * @param message 异常消息
     * @return 异常
     */
    protected Throwable error(final String message) {
        return message == null || message.isEmpty() ? new ConnectionException("Unknown error.") : new ConnectionException(message);
    }

    /**
     * 异常转换
     *
     * @param throwable 异常
     * @return 异常
     */
    protected Throwable error(final Throwable throwable) {
        return throwable == null ?
                new ConnectionException("Unknown error.") :
                new ConnectionException(throwable.getMessage(), throwable);
    }

}
