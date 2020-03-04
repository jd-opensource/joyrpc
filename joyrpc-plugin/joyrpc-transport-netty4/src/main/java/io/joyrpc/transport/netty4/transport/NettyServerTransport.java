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
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.AdapterContext;
import io.joyrpc.transport.netty4.channel.NettyChannel;
import io.joyrpc.transport.netty4.channel.NettyServerChannel;
import io.joyrpc.transport.netty4.codec.ProtocolAdapterContext;
import io.joyrpc.transport.netty4.handler.ConnectionChannelHandler;
import io.joyrpc.transport.netty4.handler.ProtocolAdapterDecoder;
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

/**
 * 服务端
 *
 * @date: 2019/2/21
 */
public class NettyServerTransport extends AbstractServerTransport {

    protected final BiFunction<Channel, URL, ChannelTransport> function;

    protected final Supplier<List<Channel>> supplier = this::getChannels;

    /**
     * 构造函数
     *
     * @param url
     * @param function
     */
    public NettyServerTransport(final URL url,
                                final BiFunction<Channel, URL, ChannelTransport> function) {
        super(url);
        this.function = function;
    }

    /**
     * 构造函数
     *
     * @param url
     * @param beforeOpen
     * @param afterClose
     * @param function
     */
    public NettyServerTransport(final URL url,
                                final Function<ServerTransport, CompletableFuture<Void>> beforeOpen,
                                final Function<ServerTransport, CompletableFuture<Void>> afterClose,
                                final BiFunction<Channel, URL, ChannelTransport> function) {
        super(url, beforeOpen, afterClose);
        this.function = function;
    }

    @Override
    protected void bind(final String host, final int port, final Consumer<AsyncResult<Channel>> consumer) {
        //消费者不会为空
        if (codec == null && adapter == null) {
            consumer.accept(new AsyncResult<>(new ConnectionException(
                    String.format("Failed binding server at %s:%d, caused by codec or adapter can not be null!",
                            host, port))));
        } else {
            try {
                SslContext sslContext = SslContextManager.getServerSslContext(url);
                EventLoopGroup bossGroup = EventLoopGroupFactory.getParentEventLoopGroup(url);
                EventLoopGroup workerGroup = EventLoopGroupFactory.getChildEventLoopGroup(url);
                ServerBootstrap bootstrap = configure(new ServerBootstrap().group(bossGroup, workerGroup), sslContext);
                bootstrap.bind(new InetSocketAddress(host, port)).addListener((ChannelFutureListener) f -> {
                    NettyServerChannel channel = new NettyServerChannel(f.channel(), bossGroup, workerGroup, supplier);
                    if (f.isSuccess()) {
                        consumer.accept(new AsyncResult<>(channel));
                    } else {
                        //自动解绑
                        Throwable error = f.cause();
                        channel.close(o -> consumer.accept(new AsyncResult<>(
                                new ConnectionException(
                                        String.format("Failed binding server at %s:%d, caused by %s",
                                                host, port, error.getMessage()), error))));
                    }
                });
            } catch (Throwable e) {
                consumer.accept(new AsyncResult<>(e));
            }
        }
    }

    /**
     * 配置
     *
     * @param bootstrap
     * @param sslContext
     */
    protected ServerBootstrap configure(final ServerBootstrap bootstrap, final SslContext sslContext) {
        //io.netty.bootstrap.Bootstrap - Unknown channel option 'SO_BACKLOG' for channel
        bootstrap.channel(Constants.isUseEpoll(url) ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new MyChannelInitializer(url, sslContext))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, url.getPositiveInt(Constants.CONNECT_TIMEOUT_OPTION))
                .option(ChannelOption.SO_REUSEADDR, url.getBoolean(Constants.SO_REUSE_PORT_OPTION))
                .option(ChannelOption.SO_BACKLOG, url.getPositiveInt(Constants.SO_BACKLOG_OPTION))
                .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(url.getPositiveInt(Constants.WRITE_BUFFER_LOW_WATERMARK_OPTION),
                        url.getPositiveInt(Constants.WRITE_BUFFER_HIGH_WATERMARK_OPTION)))
                .childOption(ChannelOption.SO_RCVBUF, url.getPositiveInt(Constants.SO_RECEIVE_BUF_OPTION))
                .childOption(ChannelOption.SO_SNDBUF, url.getPositiveInt(Constants.SO_SEND_BUF_OPTION))
                .childOption(ChannelOption.SO_KEEPALIVE, url.getBoolean(Constants.SO_KEEPALIVE_OPTION))
                .childOption(ChannelOption.TCP_NODELAY, url.getBoolean(Constants.TCP_NODELAY))
                .childOption(ChannelOption.ALLOCATOR, BufAllocator.create(url));

        return bootstrap;
    }

    /**
     * 通道初始化
     */
    protected class MyChannelInitializer extends ChannelInitializer<SocketChannel> {
        /**
         * URL
         */
        protected URL url;
        /**
         * SSL上下文
         */
        protected SslContext sslContext;

        /**
         * 构造函数
         *
         * @param url
         * @param sslContext
         */
        public MyChannelInitializer(URL url, SslContext sslContext) {
            this.url = url;
            this.sslContext = sslContext;
        }

        @Override
        protected void initChannel(final SocketChannel ch) {
            //及时发送 与 缓存发送
            Channel channel = new NettyChannel(ch, true);
            //设置payload,添加业务线程池到channel
            channel.setAttribute(Channel.PAYLOAD, url.getPositiveInt(Constants.PAYLOAD))
                    .setAttribute(Channel.BIZ_THREAD_POOL, bizThreadPool, (k, v) -> v != null);
            if (sslContext != null) {
                ch.pipeline().addFirst("ssl", sslContext.newHandler(ch.alloc()));
            }
            ch.pipeline().addLast("connection", new ConnectionChannelHandler(channel, publisher) {
                @Override
                public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
                    removeChannel(channel);
                    super.channelInactive(ctx);
                    logger.info(String.format("disconnect %s", ctx.channel().remoteAddress()));
                }
            });

            if (adapter != null) {
                ch.pipeline().addLast("adapter", new ProtocolAdapterDecoder(adapter, channel));
            } else {
                AdapterContext context = new ProtocolAdapterContext(channel, ch.pipeline());
                context.bind(codec, chain);
            }

            ChannelTransport transport = function.apply(channel, url);
            channel.setAttribute(Channel.CHANNEL_TRANSPORT, transport);
            channel.setAttribute(Channel.SERVER_CHANNEL, getServerChannel());
            addChannel(channel, transport);
        }
    }

}
