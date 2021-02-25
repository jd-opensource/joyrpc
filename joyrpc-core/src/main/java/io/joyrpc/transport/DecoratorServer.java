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

import io.joyrpc.event.EventHandler;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.ProtocolDeduction;
import io.joyrpc.util.State;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 装饰服务
 */
public class DecoratorServer<T extends TransportServer> implements Server {

    protected URL url;
    protected T transport;

    public DecoratorServer(T server) {
        this(server == null ? null : server.getUrl(), server);
    }

    public DecoratorServer(URL url, T transport) {
        this.url = url;
        this.transport = transport;
    }

    @Override
    public CompletableFuture<Void> open() {
        return transport.open();
    }

    @Override
    public CompletableFuture<Void> close() {
        return transport.close();
    }

    @Override
    public List<ChannelTransport> getTransports() {
        return transport.getTransports();
    }

    @Override
    public List<Channel> getChannels() {
        return transport.getChannels();
    }

    @Override
    public void setChain(final ChannelChain chain) {
        transport.setChain(chain);
    }

    @Override
    public void setCodec(final Codec codec) {
        transport.setCodec(codec);
    }

    @Override
    public void setDeduction(final ProtocolDeduction deduction) {
        transport.setDeduction(deduction);
    }

    @Override
    public ExecutorService getWorkerPool() {
        return transport.getWorkerPool();
    }

    @Override
    public void addEventHandler(final EventHandler handler) {
        if (handler != null) {
            transport.addEventHandler(handler);
        }
    }

    @Override
    public void addEventHandler(final EventHandler... handlers) {
        if (handlers != null) {
            for (EventHandler eventHandler : handlers) {
                addEventHandler(eventHandler);
            }
        }
    }

    @Override
    public void removeEventHandler(final EventHandler handler) {
        if (handler != null) {
            transport.removeEventHandler(handler);
        }
    }

    @Override
    public State getState() {
        return transport.getState();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return transport.getLocalAddress();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public T getTransport() {
        return transport;
    }
}
