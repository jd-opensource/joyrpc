package io.joyrpc.transport.transport;

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
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.ProtocolAdapter;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.transport.Endpoint.Status.*;
import static io.joyrpc.util.Timer.timer;

/**
 * 抽象的服务通道
 */
public abstract class AbstractServerTransport implements ServerTransport {
    private static final Logger logger = LoggerFactory.getLogger(AbstractServerTransport.class);
    protected static final AtomicReferenceFieldUpdater<AbstractServerTransport, Status> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractServerTransport.class, Status.class, "status");
    /**
     * 计数器
     */
    protected static AtomicLong COUNTER = new AtomicLong(0);
    /**
     * 编码
     */
    protected Codec codec;
    /**
     * 协议适配器
     */
    protected ProtocolAdapter adapter;
    /**
     * 处理链
     */
    protected ChannelHandlerChain chain;
    /**
     * 服务URL参数
     */
    protected URL url;
    /**
     * 监听的IP
     */
    protected String host;
    /**
     * 服务通道
     */
    protected ServerChannel serverChannel;
    /**
     * 上下文
     */
    protected Map<Channel, ChannelTransport> transports = new ConcurrentHashMap<>();
    /**
     * 业务线程池
     */
    protected ThreadPoolExecutor bizThreadPool;
    /**
     * 事件发布器
     */
    protected Publisher<TransportEvent> publisher;
    /**
     * ID
     */
    protected int transportId = ID_GENERATOR.get();
    /**
     * 打开的结果
     */
    protected volatile CompletableFuture<Channel> openFuture;
    /**
     * 关闭的结果
     */
    protected volatile CompletableFuture<Channel> closeFuture;
    /**
     * 状态
     */
    protected volatile Status status = CLOSED;

    protected BiConsumer<Channel, Consumer<AsyncResult<Channel>>> afterClose;

    /**
     * 构造函数
     *
     * @param url
     */
    public AbstractServerTransport(URL url) {
        this.url = url;
        this.host = url.getString(Constants.BIND_IP_KEY, url.getHost());
        this.publisher = EVENT_BUS.get().getPublisher(EVENT_PUBLISHER_SERVER_NAME,
                String.valueOf(COUNTER.incrementAndGet()), EVENT_PUBLISHER_TRANSPORT_CONF);
        this.afterClose = (c, t) -> {
            logger.info(String.format("Success destroying server at %s:%d", host, url.getPort()));
            status = CLOSED;
            serverChannel = null;
            publisher.close();
            Optional.ofNullable(t).ifPresent(o -> o.accept(new AsyncResult<>(c)));
            closeFuture.complete(c);
        };
    }

    @Override
    public Channel open() throws ConnectionException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionException[] ex = new ConnectionException[1];
        open(r -> {
            try {
                if (!r.isSuccess()) {
                    Throwable throwable = r.getThrowable();
                    if (throwable != null) {
                        if (throwable instanceof ConnectionException) {
                            ex[0] = (ConnectionException) throwable;
                        } else {
                            ex[0] = new ConnectionException("Server start fail !", throwable);
                        }
                    } else {
                        ex[0] = new ConnectionException("Server start fail !");
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        latch.await(url.getNaturalInt(CONNECT_TIMEOUT_OPTION), TimeUnit.MILLISECONDS);
        if (ex[0] != null) {
            throw ex[0];
        }
        return serverChannel;
    }

    @Override
    public void open(final Consumer<AsyncResult<Channel>> consumer) {
        if (STATE_UPDATER.compareAndSet(this, CLOSED, OPENING)) {
            openFuture = new CompletableFuture<>();
            bind(host, url.getPort(), r -> {
                Channel channel = r.getResult();
                if (r.isSuccess()) {
                    //成功，更新为打开状态
                    if (!STATE_UPDATER.compareAndSet(this, OPENING, OPENED)) {
                        //OPENING->CLOSING，立即释放
                        channel.close(o -> {
                            Throwable e = new IllegalStateException();
                            Optional.ofNullable(consumer).ifPresent(c -> c.accept(new AsyncResult<>(e)));
                            openFuture.completeExceptionally(e);
                        });
                    } else {
                        logger.info(String.format("Success binding server to %s:%d", host, url.getPort()));
                        serverChannel = (ServerChannel) channel;
                        publisher.start();
                        Optional.ofNullable(consumer).ifPresent(c -> c.accept(r));
                        openFuture.complete(channel);
                    }
                } else {
                    //失败
                    logger.error(String.format("Failed binding server to %s:%d", host, url.getPort()));
                    Throwable e = !STATE_UPDATER.compareAndSet(this, OPENING, CLOSED) ?
                            new IllegalStateException() : r.getThrowable();
                    Optional.ofNullable(consumer).ifPresent(c -> c.accept(new AsyncResult<>(e)));
                    openFuture.completeExceptionally(e);
                }
            });
        } else if (consumer != null) {
            switch (status) {
                case OPENING:
                case OPENED:
                    //可重入，没有并发调用
                    openFuture.whenComplete((v, t) -> consumer.accept(t != null ? new AsyncResult<>(t) : new AsyncResult<>(v)));
                    break;
                default:
                    //其它状态不应该并发执行
                    consumer.accept(new AsyncResult<>(serverChannel, new IllegalStateException()));
            }
        }
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        if (STATE_UPDATER.compareAndSet(this, OPENING, CLOSING)) {
            //处理正在打开
            closeFuture = new CompletableFuture<>();
            openFuture.whenComplete((v, t) -> afterClose.accept(v, consumer));
        } else if (STATE_UPDATER.compareAndSet(this, OPENED, CLOSING)) {
            //状态从打开到关闭中，该状态只能变更为CLOSE
            closeFuture = new CompletableFuture<>();
            openFuture.whenComplete((v, t) -> {
                if (t == null) {
                    //关闭通道
                    v.close(e -> afterClose.accept(v, consumer));
                } else {
                    //通道没有创建成功
                    afterClose.accept(v, consumer);
                }
            });
        } else {
            switch (status) {
                case CLOSING:
                    //可重入，无并发
                    closeFuture.whenComplete((v, t) -> consumer.accept(new AsyncResult<>(v)));
                default:
                    consumer.accept(new AsyncResult<>(true));
            }
        }
    }

    /**
     * 启动服务
     *
     * @param host     地址
     * @param port     端口
     * @param consumer 消费者，不会为空
     */
    protected abstract void bind(String host, int port, Consumer<AsyncResult<Channel>> consumer);

    @Override
    public Status getStatus() {
        return status;
    }

    /**
     * 返回所有的Channel
     *
     * @return
     */
    public List<Channel> getChannels() {
        return new ArrayList<>(transports.keySet());
    }

    @Override
    public List<ChannelTransport> getChannelTransports() {
        return new ArrayList<>(transports.values());
    }

    @Override
    public ServerChannel getServerChannel() {
        return serverChannel;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return serverChannel.getLocalAddress();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setChannelHandlerChain(final ChannelHandlerChain chain) {
        this.chain = chain;
    }

    @Override
    public void setCodec(final Codec codec) {
        this.codec = codec;
    }

    @Override
    public void setAdapter(final ProtocolAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void setBizThreadPool(final ThreadPoolExecutor bizThreadPool) {
        this.bizThreadPool = bizThreadPool;
    }

    @Override
    public ThreadPoolExecutor getBizThreadPool() {
        return this.bizThreadPool;
    }

    @Override
    public void addEventHandler(final EventHandler handler) {
        publisher.addHandler(handler);
    }

    @Override
    public void removeEventHandler(final EventHandler handler) {
        publisher.removeHandler(handler);
    }

    @Override
    public int getTransportId() {
        return transportId;
    }

    /**
     * 绑定Channel和Transport
     *
     * @param channel
     * @param transport
     */
    protected void addChannel(final Channel channel, final ChannelTransport transport) {
        if (channel != null && transport != null) {
            transports.put(channel, transport);
            timer().add(new EvictSessionTask(channel));
        }
    }

    /**
     * 删除Channel
     *
     * @param channel
     */
    protected void removeChannel(final Channel channel) {
        if (channel != null) {
            transports.remove(channel);
        }
    }

    /**
     * 清理过期的任务
     */
    protected static class EvictSessionTask implements Timer.TimeTask {
        /**
         * 通道
         */
        protected Channel channel;
        /**
         * 任务名称
         */
        protected String name;

        /**
         * 构造函数
         *
         * @param channel
         */
        public EvictSessionTask(Channel channel) {
            this.channel = channel;
            this.name = this.getClass().getSimpleName() + "-" + Channel.toString(channel.getRemoteAddress());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getTime() {
            return 10000L;
        }

        @Override
        public void run() {
            if (channel.isActive()) {
                channel.evictSession();
                timer().add(this);
            }
        }
    }

}
