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

import io.joyrpc.event.AsyncResult;
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.OverloadException;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.channel.SendResult;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.joyrpc.transport.session.SessionManager;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @date: 2019/1/15
 */
public class NettyChannel implements Channel {

    protected io.netty.channel.Channel channel;

    protected AtomicInteger idGenerator = new AtomicInteger(0);

    protected FutureManager<Integer, Message> futureManager = new FutureManager<>(() -> idGenerator.incrementAndGet());

    protected SessionManager sessionManager;

    protected boolean isServer;

    public NettyChannel(io.netty.channel.Channel channel) {
        this.channel = channel;
        this.isServer = Boolean.TRUE.equals(channel.attr(AttributeKey.valueOf(Channel.IS_SERVER)).get());
        this.sessionManager = new SessionManager(isServer);
        this.setAttribute(Channel.HEARTBEAT_FAILED_COUNT, new AtomicInteger(0));
    }

    @Override
    public void send(final Object object, final Consumer<SendResult> consumer) {
        if (consumer != null) {
            channel.writeAndFlush(object).addListener((future) -> {
                if (future.isSuccess()) {
                    consumer.accept(new SendResult(true, this, object));
                } else {
                    consumer.accept(new SendResult(future.cause(), this, object));
                }
            });
        } else {
            channel.writeAndFlush(object, channel.voidPromise());
        }
    }

    @Override
    public boolean close() {
        return execute(channel::close);
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        execute(channel::close, consumer);
    }

    @Override
    public boolean disconnect() {
        return execute(channel::disconnect);
    }

    @Override
    public void disconnect(final Consumer<AsyncResult<Channel>> consumer) {
        execute(channel::disconnect, consumer);
    }

    /**
     * 执行
     *
     * @param supplier
     * @return
     */
    protected boolean execute(final Supplier<ChannelFuture> supplier) {
        ChannelFuture future = supplier.get();
        try {
            future.await();
        } catch (InterruptedException e) {
        }
        return future.isSuccess();
    }

    /**
     * 执行
     *
     * @param supplier
     * @param consumer
     */
    protected void execute(final Supplier<ChannelFuture> supplier, final Consumer<AsyncResult<Channel>> consumer) {
        ChannelFuture future = supplier.get();
        if (consumer != null) {
            future.addListener(f -> {
                if (f.isSuccess()) {
                    consumer.accept(new AsyncResult<>(this));
                } else {
                    consumer.accept(new AsyncResult<>(this, f.cause()));
                }
            });
        }
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
    public void setAttribute(final String key, final Object value) {
        if (key == null) {
            return;
        }
        channel.attr(AttributeKey.valueOf(key)).set(value);
    }

    @Override
    public Object removeAttribute(final String key) {
        if (key == null) {
            return null;
        }
        return channel.attr(AttributeKey.valueOf(key)).getAndRemove();
    }

    @Override
    public FutureManager<Integer, Message> getFutureManager() {
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
    public boolean isServer() {
        return isServer;
    }

    @Override
    public void fireCaught(Throwable cause) {
        channel.pipeline().fireExceptionCaught(cause);
    }
}
