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


import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.util.State;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Endpoint
 */
public interface Endpoint {

    /**
     * 打开
     *
     * @return CompletableFuture
     */
    CompletableFuture<Channel> open();

    /**
     * 关闭
     *
     * @return CompletableFuture
     */
    CompletableFuture<Channel> close();

    /**
     * 获取本地地址
     *
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取当前状态
     *
     * @return 状态查询
     */
    State getState();

    /**
     * 绑定初始 ChannelHandlerChain
     *
     * @param chain
     */
    void setChannelHandlerChain(ChannelChain chain);

    /**
     * 绑定初始 codec
     *
     * @param codec
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
    default void runAsync(Runnable runnable) {
        ThreadPoolExecutor executor = getBizThreadPool();
        if (executor != null) {
            executor.execute(runnable);
        } else {
            CompletableFuture.runAsync(runnable);
        }
    }

}
