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
import io.joyrpc.exception.TransportException;
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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @date: 2019/2/21
 */
public class NettyServerTransport extends AbstractServerTransport {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerTransport.class);

    protected BiFunction<Channel, URL, ChannelTransport> function;

    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;

    public NettyServerTransport(URL url, BiFunction<Channel, URL, ChannelTransport> function) {
        super(url);
        this.function = function;
    }

    @Override
    protected void openChannel(final Consumer<AsyncResult<Channel>> consumer) {
        //消费者不会为空
        if (codec == null && adapter == null) {
            consumer.accept(new AsyncResult<>(
                    new ConnectionException("Failed opening server at " + url.toString(false, false) +
                            ", codec or adapter can not be null!")));
        } else {
            try {
                SslContext sslContext = SslContextManager.getServerSslContext(url);
                bossGroup = EventLoopGroupFactory.getParentEventLoopGroup(url);
                workerGroup = EventLoopGroupFactory.getChildEventLoopGroup(url);
                ServerBootstrap serverBootstrap = configure(new ServerBootstrap(), sslContext);

                // 绑定到全部网卡 或者 指定网卡
                serverBootstrap.bind(new InetSocketAddress(url.getHost(), url.getPort())).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        logger.info(String.format("Success binding server to %s", url.getAddress()));
                        consumer.accept(new AsyncResult<>(new NettyServerChannel(f.channel(), serverChannelContext)));
                    } else {
                        logger.error(String.format("Failed binding server to %s", url.getAddress()));
                        consumer.accept(new AsyncResult<>(new ConnectionException("Server start fail !", f.cause())));
                    }
                });
            } catch (SslException e) {
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
        boolean reusePort = url.getBoolean(Constants.REUSE_PORT_KEY, Constants.isLinux(url) ? Boolean.FALSE : Boolean.TRUE);

        //io.netty.bootstrap.Bootstrap - Unknown channel option 'SO_BACKLOG' for channel
        bootstrap.group(bossGroup, workerGroup)
                .channel(Constants.isUseEpoll(url) ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new MyChannelInitializer(url, sslContext))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, url.getPositiveInt(Constants.CONNECT_TIMEOUT_OPTION))
                .option(ChannelOption.SO_REUSEADDR, reusePort)   //disable this on windows, open it on linux
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

    @Override
    public void closeChannel(final Consumer<AsyncResult<Channel>> consumer) {
        logger.info(String.format("destroy the server at port:%d now...", url.getPort()));
        status.set(Status.CLOSING);
        List<Future> futures = new LinkedList<>();
        if (bossGroup != null) {
            futures.add(bossGroup.shutdownGracefully());
        }
        if (workerGroup != null) {
            futures.add(workerGroup.shutdownGracefully());
        }
        if (consumer != null && futures.isEmpty()) {
            //不需要等到
            consumer.accept(new AsyncResult<>(getServerChannel()));
        } else if (consumer != null) {
            //等待线程关闭
            LinkedList<Throwable> throwables = new LinkedList<>();
            AtomicInteger counter = new AtomicInteger(futures.size());
            for (Future future : futures) {
                future.addListener(f -> {
                    if (!f.isSuccess()) {
                        throwables.add(f.cause() == null ? new TransportException(("unknown exception.")) : f.cause());
                    }
                    if (counter.decrementAndGet() == 0) {
                        if (!throwables.isEmpty()) {
                            consumer.accept(new AsyncResult<>(getServerChannel(), throwables.peek()));
                        } else {
                            consumer.accept(new AsyncResult<>(getServerChannel()));
                        }
                    }
                });
            }
        }
        status.set(Status.CLOSED);
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
        protected void initChannel(final SocketChannel ch) throws Exception {
            ch.attr(AttributeKey.valueOf(Channel.IS_SERVER)).set(true);
            //及时发送 与 缓存发送
            Channel channel = new NettyChannel(ch);
            //设置payload
            channel.setAttribute(Channel.PAYLOAD, url.getPositiveInt(Constants.PAYLOAD));
            //添加业务线程池到channel
            if (bizThreadPool != null) {
                channel.setAttribute(Channel.BIZ_THREAD_POOL, bizThreadPool);
            }

            if (sslContext != null) {
                ch.pipeline().addFirst("ssl", sslContext.newHandler(ch.alloc()));
            }

            ch.pipeline().addLast("connection", new ConnectionChannelHandler(channel, eventPublisher) {

                @Override
                public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
                    serverChannelContext.removeChannel(channel);
                    super.channelInactive(ctx);
                }
            });

            if (adapter != null) {
                ch.pipeline().addLast("adapter", new ProtocolAdapterDecoder(adapter, channel));
            } else {
                AdapterContext context = new ProtocolAdapterContext(channel, ch.pipeline());
                context.bind(codec, handlerChain);
            }

            ChannelTransport transport = function.apply(channel, url);
            channel.setAttribute(Channel.CHANNEL_TRANSPORT, transport);
            channel.setAttribute(Channel.SERVER_CHANNEL, getServerChannel());
            serverChannelContext.addChannel(channel, transport);
        }
    }
}
