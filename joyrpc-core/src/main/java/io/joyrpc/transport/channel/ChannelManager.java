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


import io.joyrpc.event.Publisher;
import io.joyrpc.transport.TransportClient;
import io.joyrpc.transport.event.TransportEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 客户端连接通道管理器
 */
public interface ChannelManager {

    /**
     * 获取连接通道并进行连接
     *
     * @param client    客户端
     * @param connector 连接器
     * @return CompletableFuture
     */
    CompletableFuture<Channel> connect(TransportClient client, Connector connector);

    /**
     * 获取名称
     *
     * @param transport 通道
     * @return key
     */
    String getName(TransportClient transport);

    /**
     * 连接器
     */
    @FunctionalInterface
    interface Connector {

        /**
         * 创建Channel
         *
         * @param name      名称
         * @param publisher 事件发布器
         * @return CompletableFuture
         */
        CompletableFuture<Channel> connect(String name, Publisher<TransportEvent> publisher);
    }
}
