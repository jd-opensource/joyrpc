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

import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 传输通道工厂类
 */
@Extensible("transportFactory")
public interface TransportFactory {

    /**
     * 构造客户端Transport对象
     *
     * @param url URL
     * @return
     */
    ClientTransport createClientTransport(URL url);

    /**
     * 构造服务端Transport对象
     *
     * @param url URL
     * @return
     */
    ServerTransport createServerTransport(URL url);

    /**
     * 构造服务端Transport对象
     *
     * @param url        URL
     * @param beforeOpen
     * @param afterClose
     * @return
     */
    ServerTransport createServerTransport(URL url,
                                          Function<ServerTransport, CompletableFuture<Void>> beforeOpen,
                                          Function<ServerTransport, CompletableFuture<Void>> afterClose);

    /**
     * 构造Channel的Transport对象
     *
     * @param channel
     * @param url
     * @return
     */
    ChannelTransport createChannelTransport(Channel channel, URL url);
}
