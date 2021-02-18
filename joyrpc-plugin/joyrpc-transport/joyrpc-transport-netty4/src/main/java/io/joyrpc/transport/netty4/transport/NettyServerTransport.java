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
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.netty4.channel.NettyChannel;
import io.joyrpc.transport.netty4.channel.NettyServerChannel;
import io.joyrpc.transport.netty4.codec.NettyDeductionContext;
import io.joyrpc.transport.netty4.handler.ConnectionHandler;
import io.joyrpc.transport.netty4.handler.ProtocolDeductionHandler;
import io.joyrpc.transport.netty4.ssl.SslContextManager;
import io.joyrpc.transport.transport.AbstractServerTransport;
import io.joyrpc.transport.transport.ChannelTransport;
import io.joyrpc.transport.transport.ServerTransport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.joyrpc.transport.codec.ProtocolDeduction.PROTOCOL_DEDUCTION_HANDLER;

/**
 * Netty服务端
 */
public class NettyServerTransport extends AbstractServerTransport {

    /**
     * transport函数
     */
    protected final BiFunction<Channel, URL, ChannelTransport> function;
    /**
     * 连接通道列表提供者
     */
    protected final Supplier<List<Channel>> supplier = this::getChannels;
    /**
     * 断开连接处理器
     */
    protected final Consumer<Channel> inactive = this::removeChannel;

    /**
     * 构造函数
     *
     * @param url      url
     * @param function transport函数
     */
    public NettyServerTransport(final URL url,
                                final BiFunction<Channel, URL, ChannelTransport> function) {
        super(url);
        this.function = function;
    }

    /**
     * 构造函数
     *
     * @param url        url
     * @param beforeOpen 打开前
     * @param afterClose 打开后
     * @param function   transport函数
     */
    public NettyServerTransport(final URL url,
                                final Function<ServerTransport, CompletableFuture<Void>> beforeOpen,
                                final Function<ServerTransport, CompletableFuture<Void>> afterClose,
                                final BiFunction<Channel, URL, ChannelTransport> function) {
        super(url, beforeOpen, afterClose);
        this.function = function;
    }

    @Override
    protected CompletableFuture<Channel> bind(final String host, final int port) {
        CompletableFuture<Channel> future = new CompletableFuture<>();
        //消费者不会为空
        if (codec == null && deduction == null) {
            future.completeExceptionally(new ConnectionException(
                    String.format("Failed binding server at %s:%d, caused by codec or adapter can not be null!",
                            host, port)));
        } else {
            try {
                SslContext sslContext = SslContextManager.getServerSslContext(url);
                EventLoopGroup bossGroup = EventLoopGroupFactory.getBossGroup(url);
                EventLoopGroup workerGroup = EventLoopGroupFactory.getWorkerGroup(url);
                ServerBootstrap bootstrap = configure(new ServerBootstrap().group(bossGroup, workerGroup), sslContext);
                bootstrap.bind(new InetSocketAddress(host, port)).addListener((ChannelFutureListener) f -> {
                    NettyServerChannel channel = new NettyServerChannel(f.channel(), bossGroup, workerGroup, supplier);
                    if (f.isSuccess()) {
                        future.complete(channel);
                    } else {
                        //自动解绑
                        Throwable error = f.cause();
                        channel.close().whenComplete((v, e) -> future.completeExceptionally(new ConnectionException(
                                String.format("Failed binding server at %s:%d, caused by %s",
                                        host, port, error.getMessage()), error)));
                    }
                });
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }
        return future;
    }

    /**
     * 配置
     *
     * @param bootstrap  启动
     * @param sslContext ssl上下文
     */
    protected ServerBootstrap configure(final ServerBootstrap bootstrap, final SslContext sslContext) {
        //io.netty.bootstrap.Bootstrap - Unknown channel option 'SO_BACKLOG' for channel
        bootstrap.channel(Constants.isUseEpoll(url) ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        configure(ch, sslContext);
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, url.getPositiveInt(Constants.CONNECT_TIMEOUT_OPTION))
                .option(ChannelOption.SO_REUSEADDR, url.getBoolean(Constants.SO_REUSE_PORT_OPTION))
                .option(ChannelOption.SO_BACKLOG, url.getPositiveInt(Constants.SO_BACKLOG_OPTION))
                .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(
                                url.getPositiveInt(Constants.WRITE_BUFFER_LOW_WATERMARK_OPTION),
                                url.getPositiveInt(Constants.WRITE_BUFFER_HIGH_WATERMARK_OPTION)))
                .childOption(ChannelOption.SO_RCVBUF, url.getPositiveInt(Constants.SO_RECEIVE_BUF_OPTION))
                .childOption(ChannelOption.SO_SNDBUF, url.getPositiveInt(Constants.SO_SEND_BUF_OPTION))
                .childOption(ChannelOption.SO_KEEPALIVE, url.getBoolean(Constants.SO_KEEPALIVE_OPTION))
                .childOption(ChannelOption.TCP_NODELAY, url.getBoolean(Constants.TCP_NODELAY))
                .childOption(ChannelOption.ALLOCATOR, BufAllocator.create(url));

        return bootstrap;
    }

    /**
     * 配置Socket连接通道
     *
     * @param ch         Socket连接通道
     * @param sslContext ssl上下文
     * @throws Exception
     */
    protected void configure(final SocketChannel ch, final SslContext sslContext) throws Exception {
        //及时发送 与 缓存发送
        Channel channel = new NettyChannel(ch, true);
        ChannelTransport transport = function.apply(channel, url);
        //设置payload,添加业务线程池到channel
        channel.setAttribute(Channel.PAYLOAD, url.getPositiveInt(Constants.PAYLOAD)).
                setAttribute(Channel.BIZ_THREAD_POOL, bizThreadPool, (k, v) -> v != null).
                setAttribute(Channel.CHANNEL_TRANSPORT, transport).
                setAttribute(Channel.SERVER_CHANNEL, getServerChannel());
        if (sslContext != null) {
            ch.pipeline().addFirst("ssl", sslContext.newHandler(ch.alloc()));
        }
        ch.pipeline().addLast("connection", new ServerConnectionHandler(channel, publisher, inactive));
        if (deduction != null) {
            ch.pipeline().addLast(PROTOCOL_DEDUCTION_HANDLER, new ProtocolDeductionHandler(deduction, channel));
        } else {
            NettyDeductionContext.create(channel, ch.pipeline()).bind(codec, chain);
        }
        addChannel(channel, transport);
    }

    /**
     * 服务端连接处理器
     */
    protected static class ServerConnectionHandler extends ConnectionHandler {
        /**
         * 断开连接的消费者
         */
        protected final Consumer<Channel> inactive;

        public ServerConnectionHandler(final Channel channel,
                                       final Publisher<TransportEvent> publisher,
                                       final Consumer<Channel> inactive) {
            super(channel, publisher);
            this.inactive = inactive;
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            if (inactive != null) {
                inactive.accept(channel);
            }
            super.channelInactive(ctx);
            logger.info(String.format("disconnect %s", ctx.channel().remoteAddress()));
        }
    }

}
