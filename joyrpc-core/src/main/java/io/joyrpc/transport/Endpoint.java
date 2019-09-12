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


import io.joyrpc.event.AsyncResult;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.exception.TransportException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * Endpoint
 */
public interface Endpoint extends AutoCloseable {

    /**
     * 打开一个channel
     *
     * @return
     * @throws ConnectionException
     * @throws InterruptedException
     */
    Channel open() throws ConnectionException, InterruptedException;

    /**
     * 异步打开channel
     *
     * @param consumer
     * @throws ConnectionException
     */
    void open(Consumer<AsyncResult<Channel>> consumer);

    /**
     * 同步关闭
     */
    @Override
    default void close() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Exception[] exceptions = new Exception[1];
        close(r -> {
            if (!r.isSuccess()) {
                Throwable throwable = r.getThrowable();
                exceptions[0] = new TransportException(throwable == null ? "unknown exception." : throwable.getMessage(), throwable);
            }
            latch.countDown();
        });
        latch.await();
        if (exceptions[0] != null) {
            throw exceptions[0];
        }

    }

    /**
     * 异步关闭
     *
     * @param consumer
     */
    void close(Consumer<AsyncResult<Channel>> consumer);

    /**
     * 获取本地地址
     *
     * @return
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取当前状态
     *
     * @return
     */
    Status getStatus();

    /**
     * 绑定初始 ChannelHandlerChain
     *
     * @param chain
     */
    void setChannelHandlerChain(ChannelHandlerChain chain);

    /**
     * 绑定初始 codec
     *
     * @param codec
     */
    void setCodec(Codec codec);

    /**
     * 设置业务线程池
     *
     * @param bizThreadPool
     */
    void setBizThreadPool(ThreadPoolExecutor bizThreadPool);

    /**
     * 获取当前配置的线程池
     *
     * @return
     */
    ThreadPoolExecutor getBizThreadPool();

    /**
     * 线程池异步执行
     *
     * @param runnable
     */
    default void runAsync(Runnable runnable) {
        ThreadPoolExecutor executor = getBizThreadPool();
        if (executor != null) {
            executor.execute(runnable);
        } else {
            CompletableFuture.runAsync(runnable);
        }
    }

    /**
     * @date: 2019/1/14
     */
    enum Status {
        CLOSED, OPENING, OPENED, CLOSING
    }
}
