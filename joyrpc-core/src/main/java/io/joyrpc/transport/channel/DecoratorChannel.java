package io.joyrpc.transport.channel;

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

import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.SessionManager;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 通道装饰器
 */
public class DecoratorChannel implements Channel {

    protected Channel channel;

    public DecoratorChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(final Object object, final Consumer<SendResult> consumer) {
        channel.send(object, consumer);
    }

    @Override
    public CompletableFuture<Channel> close() {
        return channel.close();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
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
        return channel.getAttribute(key);
    }

    @Override
    public Channel setAttribute(final String key, final Object value) {
        return channel.setAttribute(key, value);
    }

    @Override
    public Object removeAttribute(final String key) {
        return channel.removeAttribute(key);
    }

    @Override
    public FutureManager<Long, Message> getFutureManager() {
        return channel.getFutureManager();
    }

    @Override
    public ChannelBuffer buffer() {
        return channel.buffer();
    }

    @Override
    public ChannelBuffer buffer(int initialCapacity) {
        return channel.buffer(initialCapacity);
    }

    @Override
    public ChannelBuffer buffer(int initialCapacity, int maxCapacity) {
        return channel.buffer(initialCapacity, maxCapacity);
    }

    @Override
    public SessionManager getSessionManager() {
        return channel.getSessionManager();
    }

    @Override
    public boolean isServer() {
        return channel.isServer();
    }

    @Override
    public void fireCaught(Throwable caught) {
        channel.fireCaught(caught);
    }
}
