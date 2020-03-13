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
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.transport.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @date: 2019/2/21
 */
@Extension("netty4")
@ConditionalOnClass("io.netty.channel.Channel")
public class NettyTransportFactory implements TransportFactory {

    @Override
    public ClientTransport createClientTransport(final URL url) {
        return new NettyClientTransport(url);
    }

    @Override
    public ServerTransport createServerTransport(final URL url) {
        return new NettyServerTransport(url, this::createChannelTransport);
    }

    @Override
    public ServerTransport createServerTransport(final URL url,
                                                 final Function<ServerTransport, CompletableFuture<Void>> beforeOpen,
                                                 final Function<ServerTransport, CompletableFuture<Void>> afterClose) {
        return new NettyServerTransport(url, beforeOpen, afterClose, this::createChannelTransport);
    }

    @Override
    public ChannelTransport createChannelTransport(final Channel channel, final URL url) {
        return new DefaultChannelTransport(channel, url);
    }

}
