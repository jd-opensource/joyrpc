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


import io.joyrpc.transport.transport.ClientTransport;

import java.util.concurrent.CompletableFuture;

/**
 * 连接通道管理器
 */
public interface ChannelManager {

    /**
     * 获取channel
     *
     * @param transport 客户端通道
     * @param connector 连接器
     * @return CompletableFuture
     */
    CompletableFuture<Channel> getChannel(ClientTransport transport, Connector connector);

    /**
     * 获取存储channel的key
     *
     * @param transport 通道
     * @return key
     */
    String getChannelKey(ClientTransport transport);

    /**
     * 连接器
     */
    @FunctionalInterface
    interface Connector {

        /**
         * 创建Channel
         */
        CompletableFuture<Channel> connect();
    }
}
