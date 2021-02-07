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
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.ProtocolDeduction;
import io.joyrpc.transport.transport.ChannelTransport;
import io.joyrpc.transport.transport.ServerTransport;
import io.joyrpc.util.State;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 装饰服务
 */
public class DecoratorServer<T extends ServerTransport> implements Server {

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
    public CompletableFuture<Channel> open() {
        return transport.open();
    }

    @Override
    public CompletableFuture<Channel> close() {
        return transport.close();
    }

    @Override
    public List<ChannelTransport> getChannelTransports() {
        return transport.getChannelTransports();
    }

    @Override
    public ServerChannel getServerChannel() {
        return transport.getServerChannel();
    }

    @Override
    public void setChannelHandlerChain(final ChannelChain chain) {
        transport.setChannelHandlerChain(chain);
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
    public void setBizThreadPool(final ThreadPoolExecutor threadPool) {
        transport.setBizThreadPool(threadPool);
    }

    @Override
    public ThreadPoolExecutor getBizThreadPool() {
        return transport.getBizThreadPool();
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
