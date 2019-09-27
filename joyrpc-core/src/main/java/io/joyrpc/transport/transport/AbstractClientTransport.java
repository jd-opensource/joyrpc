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
import io.joyrpc.transport.channel.ChannelManager.ChannelOpener;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.event.ReconnectedEvent;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.DefaultHeartbeatTrigger;
import io.joyrpc.transport.heartbeat.HeartbeatManager;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.message.Message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;

/**
 * 抽象的客户端通道
 */
public abstract class AbstractClientTransport extends DefaultChannelTransport implements ClientTransport {
    /**
     * 状态
     */
    protected AtomicReference<Status> status = new AtomicReference<>(Status.CLOSED);
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
     * 心跳管理器
     */
    protected HeartbeatManager heartbeatManager;
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
     * 构造函数
     *
     * @param url
     */
    public AbstractClientTransport(URL url) {
        super(url);
        String channelManagerFactory = url.getString(CHANNEL_MANAGER_FACTORY_OPTION);
        this.channelManager = CHANNEL_MANAGER_FACTORY.getOrDefault(channelManagerFactory).getChannelManager(url);
        this.channelName = channelManager.getChannelKey(this);
        this.publisher = EVENT_BUS.get().getPublisher(EVENT_PUBLISHER_CLIENT_NAME, channelName, EVENT_PUBLISHER_TRANSPORT_CONF);
        this.heartbeatManager = HEARTBEAT_MANAGER_FACTORY.get().get(url);
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
                            ex[0] = new ConnectionException("Failed to connect " + url.toString(false, false)
                                    + ". Cause by: Remote and local address are the same", r.getThrowable());
                        }
                    } else {
                        ex[0] = new ConnectionException("Failed to connect " + url.toString(false, false)
                                + ". Cause by: Remote and local address are the same");
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
        if (status.compareAndSet(Status.CLOSED, Status.OPENING)) {
            channelManager.getChannel(this, r -> {
                if (r.isSuccess()) {
                    status.set(Status.OPENED);
                    channel = r.getResult();
                } else {
                    status.set(Status.CLOSED);
                }
                if (consumer != null) {
                    consumer.accept(r);
                }
            }, getChannelOpener());
        } else if (consumer != null) {
            switch (status.get()) {
                case OPENED:
                    consumer.accept(new AsyncResult<>(channel));
                    break;
                default:
                    consumer.accept(new AsyncResult<>(channel, new IllegalStateException()));
            }
        }
    }

    protected abstract ChannelOpener getChannelOpener();

    @Override
    public void reconnect() throws ConnectionException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConnectionException[] ex = new ConnectionException[1];
        reconnect((ch, err) -> {
            try {
                if (err != null) {
                    ex[0] = err instanceof ConnectionException ? (ConnectionException) err : new ConnectionException(err);
                }
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (ex[0] != null) {
            throw ex[0];
        }
    }

    @Override
    public void reconnect(final BiConsumer<Channel, Throwable> action) {
        if (status.get() == Status.OPENED) {
            //先关闭channel
            channel.close(s -> {
                if (s.isSuccess()) {
                    //重新获取channel
                    channelManager.getChannel(this, r -> {
                        if (r.isSuccess()) {
                            channel = r.getResult();
                            //TODO 是否要reconnectedEvent?
                            publisher.offer(new ReconnectedEvent(r.getResult(), url));
                            if (action != null) {
                                action.accept(r.getResult(), null);
                            }
                        } else {
                            publisher.offer(new ReconnectedEvent(r.getResult(), url, r.getThrowable()));
                            if (action != null) {
                                action.accept(r.getResult(), r.getThrowable());
                            }
                        }
                    }, getChannelOpener());
                } else if (action != null) {
                    action.accept(s.getResult(), s.getThrowable());
                }
            });
        } else {
            if (action != null) {
                action.accept(channel, new IllegalStateException());
            }
        }


    }

    @Override
    public boolean disconnect() {
        return status.get() == Status.OPENED && channel.disconnect();
    }

    @Override
    public void disconnect(final Consumer<AsyncResult<Channel>> consumer) {
        if (status.get() == Status.OPENED) {
            channel.disconnect(o -> {
                consumer.accept(o);
            });
        } else {
            consumer.accept(new AsyncResult<>(channel, new IllegalStateException()));
        }

    }

    @Override
    public void setHeartbeatStrategy(final HeartbeatStrategy heartbeatStrategy) {
        this.heartbeatStrategy = heartbeatStrategy;
        Supplier<Message> heartbeat = heartbeatStrategy.getHeartbeat();
        //channel创建的时候回自动添加心跳
        if (channel != null && heartbeat != null) {
            heartbeatManager.add(channel, heartbeatStrategy, () -> new DefaultHeartbeatTrigger(channel, url, heartbeatStrategy, publisher));
        }
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
        return status.get();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        //TODO 是否应该关闭publisher
        final Channel channel = this.channel;
        if (channel == null) {
            if (status.get() == Status.CLOSED) {
                consumer.accept(new AsyncResult<>(new IllegalStateException()));
            } else {
                status.set(Status.CLOSED);
                consumer.accept(new AsyncResult<>(true));
            }
        } else {
            status.set(Status.CLOSING);
            //共享Transport不直接关闭Channel，只是计数器减1，所以调用disconnect方法
            channel.disconnect(e -> {
                status.set(Status.CLOSED);
                this.channel.removeSession(this.transportId);
                this.channel = null;
                if (consumer != null) {
                    if (e.isSuccess()) {
                        consumer.accept(new AsyncResult<>(channel));
                    } else {
                        consumer.accept(new AsyncResult<>(channel, e.getThrowable()));
                    }
                }
            });
        }
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
