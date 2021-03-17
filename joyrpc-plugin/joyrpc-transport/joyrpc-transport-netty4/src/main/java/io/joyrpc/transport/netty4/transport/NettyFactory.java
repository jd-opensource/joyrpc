package io.joyrpc.transport.netty4.transport;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.util.thread.ThreadPool;
import io.joyrpc.transport.*;
import io.joyrpc.transport.channel.Channel;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Netty传输通道工厂
 */
@Extension("netty4")
@ConditionalOnClass("io.netty.channel.Channel")
public class NettyFactory implements TransportFactory {

    /**
     * 连接通道到传输通道的转换函数
     */
    protected static final BiFunction<Channel, URL, ChannelTransport> CHANNEL_TRANSPORT_FUNCTION = (channel, url) -> new DefaultChannelTransport(channel, url);

    @Override
    public TransportClient createClient(final URL url, final ThreadPool workerPool) {
        return new NettyClient(url, workerPool);
    }

    @Override
    public TransportServer createServer(final URL url, final ThreadPool workerPool) {
        return new NettyServer(url, workerPool, CHANNEL_TRANSPORT_FUNCTION);
    }

    @Override
    public TransportServer createServer(final URL url,
                                        final ThreadPool workerPool,
                                        final Function<TransportServer, CompletableFuture<Void>> beforeOpen,
                                        final Function<TransportServer, CompletableFuture<Void>> afterClose) {
        return new NettyServer(url, workerPool, beforeOpen, afterClose, CHANNEL_TRANSPORT_FUNCTION);
    }
}
