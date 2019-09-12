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
import io.joyrpc.event.EventBus;
import io.joyrpc.event.EventHandler;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.Endpoint;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.channel.ServerChannelContext;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.ProtocolAdapter;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.session.ServerSessionManager;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.EVENT_BUS;
import static io.joyrpc.constants.Constants.*;

/**
 * 抽象的服务通道
 */
public abstract class AbstractServerTransport implements ServerTransport {

    protected static AtomicLong counter = new AtomicLong(0);
    protected AtomicReference<Endpoint.Status> status = new AtomicReference<>(Endpoint.Status.CLOSED);
    protected Codec codec;
    protected ProtocolAdapter adapter;
    protected ChannelHandlerChain handlerChain;
    protected URL url;
    protected ServerChannel serverChannel;
    protected ServerChannelContext serverChannelContext = new ServerChannelContext();
    protected ThreadPoolExecutor bizThreadPool;
    protected EventBus eventBus;
    protected Publisher<TransportEvent> eventPublisher;
    protected int transportId = ID_GENERATOR.get();

    public AbstractServerTransport(URL url) {
        this.url = url;
        this.eventBus = EVENT_BUS.get();
        this.eventPublisher = eventBus.getPublisher(EVENT_PUBLISHER_SERVER_NAME,
                String.valueOf(counter.incrementAndGet()), EVENT_PUBLISHER_TRANSPORT_CONF);
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
        if (status.compareAndSet(Endpoint.Status.CLOSED, Endpoint.Status.OPENING)) {
            openChannel(r -> {
                if (r.isSuccess()) {
                    status.set(Endpoint.Status.OPENED);
                    eventPublisher.start();
                    serverChannel = (ServerChannel) r.getResult();
                    ServerSessionManager.getInstance().add(serverChannel);
                } else {
                    status.set(Endpoint.Status.CLOSED);
                }
                if (consumer != null) {
                    consumer.accept(r);
                }
            });
        } else {
            consumer.accept(new AsyncResult<>(serverChannel));
        }
    }

    /**
     * 创建通道
     *
     * @param consumer 消费者，不会为空
     */
    protected abstract void openChannel(Consumer<AsyncResult<Channel>> consumer);

    @Override
    public void close(Consumer<AsyncResult<Channel>> consumer) {
        if (null != bizThreadPool && !bizThreadPool.isShutdown()) {
            bizThreadPool.shutdown();
        }
        ServerSessionManager.getInstance().remove(serverChannel);
        eventPublisher.close();
        closeChannel(consumer);
    }

    protected abstract void closeChannel(Consumer<AsyncResult<Channel>> consumer);

    @Override
    public Endpoint.Status getStatus() {
        return status.get();
    }

    @Override
    public List<ChannelTransport> getChannelTransports() {
        return serverChannelContext.getChannelTransports();
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
        this.handlerChain = chain;
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
        eventPublisher.addHandler(handler);
    }

    @Override
    public void removeEventHandler(final EventHandler handler) {
        eventPublisher.removeHandler(handler);
    }

    @Override
    public int getTransportId() {
        return transportId;
    }

}
