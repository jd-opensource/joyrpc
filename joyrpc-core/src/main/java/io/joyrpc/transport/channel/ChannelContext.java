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
import io.joyrpc.transport.event.TransportEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 连接通道上下文
 */
public interface ChannelContext {

    /**
     * 获取连接通道
     *
     * @return 连接通道
     */
    Channel getChannel();

    /**
     * 传递连接事件
     */
    void fireChannelActive();

    /**
     * 传递连接断开事件
     */
    void fireChannelInactive();

    /**
     * 传递异常事件
     */
    void fireExceptionCaught(Throwable cause);

    /**
     * 传递数据读取事件
     */
    void fireChannelRead(Object msg);

    /**
     * 写并且提交数据
     *
     * @param msg 数据
     * @return CompletableFuture
     */
    CompletableFuture<Void> wrote(Object msg);

    /**
     * 发布事件
     *
     * @param event 事件
     */
    default void publish(final TransportEvent event) {
        Publisher<TransportEvent> publisher = getChannel().getPublisher();
        publisher.offer(event);
    }

}
