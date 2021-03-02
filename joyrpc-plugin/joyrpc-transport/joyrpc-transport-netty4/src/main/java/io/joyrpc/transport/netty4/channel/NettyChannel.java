package io.joyrpc.transport.netty4.channel;

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

import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.OverloadException;
import io.joyrpc.thread.ThreadPool;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.joyrpc.transport.netty4.util.FutureAdapter;
import io.joyrpc.transport.session.SessionManager;
import io.joyrpc.util.IdGenerator;
import io.joyrpc.util.IdGenerator.ClientStreamIdGenerator;
import io.joyrpc.util.IdGenerator.IntToLongIdGenerator;
import io.joyrpc.util.IdGenerator.ServerStreamIdGenerator;
import io.joyrpc.util.IdGenerator.StreamIdGenerator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Netty连接通道
 */
public class NettyChannel implements Channel {
    protected static final String SEND_REQUEST_TOO_FAST = "Send request exception, because sending request is too fast, causing channel is not writable. at %s : %s";
    protected static final String SEND_REQUEST_NOT_ACTIVE = "Send request exception, causing channel is not active. at  %s : %s";
    /**
     * 名称
     */
    protected final String name;
    /**
     * 事件发布器
     */
    protected final Publisher<TransportEvent> publisher;
    /**
     * 通道接口
     */
    protected final io.netty.channel.Channel channel;
    /**
     * 工作线程池
     */
    protected final ThreadPool workerPool;
    /**
     * 数据包大小
     */
    protected final int payloadSize;
    /**
     * 消息ID
     */
    protected final IdGenerator<Long> msgIdGenerator;
    /**
     * 消息ID
     */
    protected final StreamIdGenerator streamIdGenerator;
    /**
     * Future管理器
     */
    protected final FutureManager<Long, Message> futureManager;
    /**
     * 会话管理器
     */
    protected final SessionManager sessionManager;
    /**
     * 是否是服务端
     */
    protected final boolean server;

    /**
     * 构造函数
     *
     * @param name        名称
     * @param channel     通道
     * @param workerPool  业务线程池
     * @param publisher   事件发布器
     * @param payloadSize 数据包大小
     * @param server      服务端标识
     */
    public NettyChannel(final String name,
                        final io.netty.channel.Channel channel,
                        final ThreadPool workerPool,
                        final Publisher<TransportEvent> publisher,
                        final int payloadSize,
                        final boolean server) {
        this.name = name;
        this.channel = channel;
        this.publisher = publisher;
        this.workerPool = workerPool;
        this.payloadSize = payloadSize;
        this.server = server;
        this.msgIdGenerator = new IntToLongIdGenerator();
        this.streamIdGenerator = server ? new ServerStreamIdGenerator() : new ClientStreamIdGenerator();
        this.futureManager = new FutureManager<>(this, msgIdGenerator, streamIdGenerator);
        this.sessionManager = new SessionManager(server);
    }

    @Override
    public CompletableFuture<Void> send(final Object object) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (object == null) {
            future.complete(null);
        } else if (!channel.isWritable()) {
            LafException throwable = channel.isActive() ?
                    new OverloadException(String.format(SEND_REQUEST_TOO_FAST, Channel.toString(this), object.toString()), 0, isServer()) :
                    new ChannelClosedException(String.format(SEND_REQUEST_NOT_ACTIVE, Channel.toString(this), object.toString()));
            future.completeExceptionally(throwable);
        } else {
            try {
                //TODO 要不要改成工作线程池来回调
                channel.writeAndFlush(object).addListener(new FutureAdapter<>(future));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }
        return future;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<Channel> close() {
        CompletableFuture<Channel> future = new CompletableFuture();
        try {
            channel.close().addListener(new FutureAdapter<>(future, this));
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public <T> T getAttribute(final String key) {
        if (key == null) {
            return null;
        }
        Attribute<T> attribute = channel.attr(AttributeKey.valueOf(key));
        return attribute.get();
    }

    @Override
    public <T> T getAttribute(final String key, final Function<String, T> function) {
        if (key == null) {
            return null;
        }
        Attribute<T> attribute = channel.attr(AttributeKey.valueOf(key));
        T result = attribute.get();
        if (result == null && function != null) {
            T target = function.apply(key);
            if (target != null) {
                if (attribute.compareAndSet(null, target)) {
                    return target;
                } else {
                    return attribute.get();
                }
            }
        }
        return result;
    }

    @Override
    public Channel setAttribute(final String key, final Object value) {
        if (key != null) {
            channel.attr(AttributeKey.valueOf(key)).set(value);
        }
        return this;
    }

    @Override
    public Object removeAttribute(final String key) {
        if (key == null) {
            return null;
        }
        return channel.attr(AttributeKey.valueOf(key)).getAndSet(null);
    }

    @Override
    public FutureManager<Long, Message> getFutureManager() {
        return futureManager;
    }

    @Override
    public ChannelBuffer buffer() {
        return new NettyChannelBuffer(channel.alloc().buffer());
    }

    @Override
    public ChannelBuffer buffer(final int initialCapacity) {
        return new NettyChannelBuffer(channel.alloc().buffer(initialCapacity));
    }

    @Override
    public ChannelBuffer buffer(final int initialCapacity, final int maxCapacity) {
        return new NettyChannelBuffer(channel.alloc().buffer(initialCapacity, maxCapacity));
    }

    @Override
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public Publisher<TransportEvent> getPublisher() {
        return publisher;
    }

    @Override
    public ThreadPool getWorkerPool() {
        return workerPool;
    }

    @Override
    public int getPayloadSize() {
        return payloadSize;
    }

    @Override
    public boolean isServer() {
        return server;
    }

    @Override
    public void fireCaught(Throwable cause) {
        channel.pipeline().fireExceptionCaught(cause);
    }

}
