package io.joyrpc.transport;

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
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.transport.ClientTransport;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @date: 2019/2/21
 */
public class DecoratorClient<T extends ClientTransport> implements Client {
    /**
     * URL
     */
    protected URL url;
    /**
     * 通道
     */
    protected T transport;
    /**
     * 会话
     */
    protected Session session;
    /**
     * 协议
     */
    protected ClientProtocol protocol;

    public DecoratorClient(T client) {
        this(client == null ? null : client.getUrl(), client);
    }

    public DecoratorClient(URL url, T transport) {
        Objects.requireNonNull(transport, "transport can not be null.");
        Objects.requireNonNull(url, "url can not be null.");
        this.url = url;
        this.transport = transport;
    }

    @Override
    public Channel open() throws ConnectionException, InterruptedException {
        return transport.open();
    }

    @Override
    public void open(final Consumer<AsyncResult<Channel>> consumer) {
        transport.open(consumer);
    }

    @Override
    public Status getStatus() {
        return transport.getStatus();
    }

    @Override
    public void close() throws Exception {
        transport.close();
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        transport.close(consumer);
    }

    @Override
    public Channel getChannel() {
        return transport.getChannel();
    }

    @Override
    public CompletableFuture<Void> oneway(final Message message) {
        return transport.oneway(message);
    }

    @Override
    public Message sync(final Message message, final int timoutMillis) throws RpcException, TimeoutException {
        return transport.sync(message, timoutMillis);
    }

    @Override
    public CompletableFuture<Message> async(final Message message, final int timoutMillis) {
        return transport.async(message, timoutMillis);
    }

    @Override
    public void async(final Message message, final BiConsumer<Message, Throwable> action, final int timoutMillis) {
        transport.async(message, action, timoutMillis);
    }

    @Override
    public void setHeartbeatStrategy(final HeartbeatStrategy heartbeatStrategy) {
        transport.setHeartbeatStrategy(heartbeatStrategy);
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return transport.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return transport.getLocalAddress();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setChannelHandlerChain(final ChannelHandlerChain chain) {
        transport.setChannelHandlerChain(chain);
    }

    @Override
    public void setCodec(final Codec codec) {
        transport.setCodec(codec);
    }

    @Override
    public void setBizThreadPool(final ThreadPoolExecutor threadPool) {
        transport.setBizThreadPool(threadPool);
    }

    @Override
    public ThreadPoolExecutor getBizThreadPool() {
        return transport.getBizThreadPool();
    }

    @Override
    public long getLastRequestTime() {
        return transport.getLastRequestTime();
    }

    @Override
    public void addEventHandler(final EventHandler<? extends TransportEvent> handler) {
        transport.addEventHandler(handler);
    }

    @Override
    public void addEventHandler(final EventHandler<? extends TransportEvent>... handlers) {
        if (handlers != null) {
            for (EventHandler eventHandler : handlers) {
                addEventHandler(eventHandler);
            }
        }
    }

    @Override
    public void removeEventHandler(EventHandler<? extends TransportEvent> handler) {
        if (handler != null) {
            transport.removeEventHandler(handler);
        }
    }

    @Override
    public HeartbeatStrategy getHeartbeatStrategy() {
        return transport.getHeartbeatStrategy();
    }

    @Override
    public String getChannelName() {
        return transport.getChannelName();
    }

    @Override
    public Publisher<TransportEvent> getPublisher() {
        return transport.getPublisher();
    }

    @Override
    public Session session() {
        if (session == null) {
            session = transport.session();
        }
        return session;
    }

    @Override
    public Session session(final Session session) {
        this.session = session;
        return transport.session(session);
    }

    @Override
    public int getTransportId() {
        return transport.getTransportId();
    }

    @Override
    public void setProtocol(final ClientProtocol protocol) {
        this.protocol = protocol;
        transport.setProtocol(protocol);
    }

    @Override
    public ClientProtocol getProtocol() {
        if (protocol == null) {
            protocol = transport.getProtocol();
        }
        return protocol;
    }
}
