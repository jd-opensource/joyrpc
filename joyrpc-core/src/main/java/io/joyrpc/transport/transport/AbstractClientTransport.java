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

import io.joyrpc.event.AsyncResult;
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.channel.ChannelManager;
import io.joyrpc.transport.channel.ChannelManager.Connector;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.util.Futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.CHANNEL_MANAGER_FACTORY;
import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.transport.Endpoint.Status.*;

/**
 * 抽象的客户端通道，不支持并发打开关闭
 */
public abstract class AbstractClientTransport extends DefaultChannelTransport implements ClientTransport {

    protected static final AtomicReferenceFieldUpdater<AbstractClientTransport, Status> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractClientTransport.class, Status.class, "status");
    /**
     * 编解码
     */
    protected Codec codec;
    /**
     * 处理链
     */
    protected ChannelHandlerChain handlerChain;
    /**
     * Channel管理器
     */
    protected ChannelManager channelManager;
    /**
     * 心跳策略
     */
    protected HeartbeatStrategy heartbeatStrategy;
    /**
     * 业务线程池
     */
    protected ThreadPoolExecutor bizThreadPool;
    /**
     * 名称
     */
    protected String channelName;
    /**
     * 事件发布器
     */
    protected Publisher<TransportEvent> publisher;
    /**
     * 客户端协议
     */
    protected ClientProtocol protocol;
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

    /**
     * 构造函数
     *
     * @param url
     */
    public AbstractClientTransport(URL url) {
        super(url);
        this.channelManager = CHANNEL_MANAGER_FACTORY.getOrDefault(url.getString(CHANNEL_MANAGER_FACTORY_OPTION)).getChannelManager(url);
        this.channelName = channelManager.getChannelKey(this);
        this.publisher = EVENT_BUS.get().getPublisher(EVENT_PUBLISHER_CLIENT_NAME, channelName, EVENT_PUBLISHER_TRANSPORT_CONF);
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
                            ex[0] = new ConnectionException(throwable.getMessage(), throwable);
                        }
                    } else {
                        ex[0] = new ConnectionException("Unknown error.");
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
        return this.channel;
    }

    @Override
    public void open(final Consumer<AsyncResult<Channel>> consumer) {
        if (STATE_UPDATER.compareAndSet(this, CLOSED, OPENING)) {
            final CompletableFuture<Channel> future = new CompletableFuture<>();
            final Consumer<AsyncResult<Channel>> c = Futures.chain(consumer, openFuture);
            openFuture = future;
            channelManager.getChannel(this, r -> {
                //异步回调再次进行判断，在OPENING可以直接关闭
                if (r.isSuccess()) {
                    //成功，更新为打开状态
                    Channel ch = r.getResult();
                    if (openFuture != future || !STATE_UPDATER.compareAndSet(this, OPENING, OPENED)) {
                        //OPENING->CLOSING，自动关闭
                        ch.close(o -> c.accept(new AsyncResult<>(new ConnectionException("state is illegal."))));
                    } else {
                        //设置连接，并触发通知
                        channel = ch;
                        c.accept(r);
                    }
                } else if (openFuture != future) {
                    //失败
                    c.accept(r);
                } else {
                    //失败关闭
                    future.completeExceptionally(r.getThrowable());
                    close(o -> consumer.accept(r));
                }
            }, getConnector());
        } else if (consumer != null) {
            switch (status) {
                case OPENING:
                    Futures.chain(openFuture, consumer);
                    break;
                case OPENED:
                    //可重入，没有并发调用
                    consumer.accept(new AsyncResult<>(channel));
                    break;
                default:
                    //其它状态不应该并发执行
                    consumer.accept(new AsyncResult<>(channel, new ConnectionException("state is illegal.")));
            }
        }
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        if (STATE_UPDATER.compareAndSet(this, OPENING, CLOSING)) {
            closeFuture = new CompletableFuture<>();
            Futures.chain(openFuture, o -> doClose(Futures.chain(consumer, closeFuture)));
        } else if (STATE_UPDATER.compareAndSet(this, OPENED, CLOSING)) {
            //状态从打开到关闭中，该状态只能变更为CLOSED
            closeFuture = new CompletableFuture<>();
            doClose(Futures.chain(consumer, closeFuture));
        } else if (consumer != null) {
            switch (status) {
                case CLOSING:
                    Futures.chain(closeFuture, consumer);
                    break;
                case CLOSED:
                    consumer.accept(new AsyncResult<>(true));
                    break;
                default:
                    consumer.accept(new AsyncResult<>(new IllegalStateException("status is illegal.")));
            }
        }
    }

    /**
     * 关闭
     *
     * @param consumer
     */
    protected void doClose(final Consumer<AsyncResult<Channel>> consumer) {
        if (channel != null) {
            channel.close(r -> {
                channel.removeSession(transportId);
                //channel不设置为null，防止正在处理的请求报空指针错误
                //channel = null;
                status = CLOSED;
                consumer.accept(r);
            });
        } else {
            status = CLOSED;
            consumer.accept(new AsyncResult<>(true));
        }
    }


    /**
     * 获取连接器
     *
     * @return
     */
    protected abstract Connector getConnector();

    @Override
    public long getRequests() {
        return requests.get();
    }

    @Override
    public void setHeartbeatStrategy(final HeartbeatStrategy heartbeatStrategy) {
        this.heartbeatStrategy = heartbeatStrategy;
    }

    @Override
    public HeartbeatStrategy getHeartbeatStrategy() {
        return heartbeatStrategy;
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    @Override
    public Publisher<TransportEvent> getPublisher() {
        return publisher;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setChannelHandlerChain(final ChannelHandlerChain chain) {
        this.handlerChain = chain;
    }

    @Override
    public void setCodec(final Codec codec) {
        this.codec = codec;
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
    public void addEventHandler(final EventHandler<? extends TransportEvent> handler) {
        if (handler != null) {
            publisher.addHandler((EventHandler<TransportEvent>) handler);
        }
    }

    @Override
    public void removeEventHandler(final EventHandler<? extends TransportEvent> handler) {
        if (handler != null) {
            publisher.removeHandler((EventHandler<TransportEvent>) handler);
        }
    }

    @Override
    public ClientProtocol getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(final ClientProtocol protocol) {
        this.protocol = protocol;
        if (channel != null) {
            channel.setAttribute(Channel.PROTOCOL, protocol);
        }
    }
}
