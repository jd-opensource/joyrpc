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
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.util.IdGenerator;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * 传输通道
 */
public interface Transport {

    /**
     * 获取本地地址
     *
     * @return 本地地址
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取URL
     *
     * @return
     */
    URL getUrl();

    /**
     * 绑定处理链
     *
     * @param chain 链表
     */
    void setChain(ChannelChain chain);

    /**
     * 绑定编解码
     *
     * @param codec 编解码
     */
    void setCodec(Codec codec);

    /**
     * 设置业务线程池
     *
     * @param bizThreadPool 线程池
     */
    void setBizThreadPool(ThreadPoolExecutor bizThreadPool);

    /**
     * 获取当前配置的线程池
     *
     * @return 线程池
     */
    ThreadPoolExecutor getBizThreadPool();

    /**
     * 线程池异步执行
     *
     * @param runnable 执行块
     */
    default void runAsync(final Runnable runnable) {
        if (runnable != null) {
            ThreadPoolExecutor executor = getBizThreadPool();
            if (executor != null) {
                executor.execute(runnable);
            } else {
                CompletableFuture.runAsync(runnable);
            }
        }
    }

    /**
     * 添加一个事件处理器
     *
     * @param handler 事件处理器
     */
    default void addEventHandler(EventHandler<? extends TransportEvent> handler) {

    }

    /**
     * 添加一组事件处理器
     *
     * @param handlers 事件处理器数组
     */
    default void addEventHandler(EventHandler<? extends TransportEvent>... handlers) {
        if (handlers != null) {
            for (EventHandler<? extends TransportEvent> handler : handlers) {
                addEventHandler(handler);
            }
        }
    }

    /**
     * 移除一个事件处理器
     *
     * @param handler 事件处理器
     */
    default void removeEventHandler(EventHandler<? extends TransportEvent> handler) {

    }

    /**
     * 获取通道序号
     *
     * @return 通道序号
     */
    default int getTransportId() {
        return 0;
    }

    /**
     * 传输通道序号生成器
     */
    Supplier<Integer> ID_GENERATOR = new IdGenerator.IntIdGenerator();

}
