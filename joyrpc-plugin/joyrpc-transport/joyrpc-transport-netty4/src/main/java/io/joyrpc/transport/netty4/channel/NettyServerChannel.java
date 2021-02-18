package io.joyrpc.transport.netty4.channel;

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

import io.joyrpc.exception.TransportException;
import io.joyrpc.transport.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty服务端连接通道
 */
public class NettyServerChannel extends NettyChannel {
    /**
     * boss线程池
     */
    protected EventLoopGroup bossGroup;
    /**
     * 工作线程池
     */
    protected EventLoopGroup workerGroup;

    /**
     * 构造函数
     *
     * @param channel     Netty连接通道
     * @param bossGroup   主线程池
     * @param workerGroup 工作线程池
     */
    public NettyServerChannel(final io.netty.channel.Channel channel,
                              final EventLoopGroup bossGroup,
                              final EventLoopGroup workerGroup) {
        super(channel, true);
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
    }

    @Override
    public CompletableFuture<Channel> close() {
        CompletableFuture<Channel> result = new CompletableFuture<>();
        super.close().whenComplete((ch, error) -> {
            List<Future> futures = new LinkedList<>();
            if (bossGroup != null) {
                futures.add(bossGroup.shutdownGracefully());
            }
            if (workerGroup != null) {
                futures.add(workerGroup.shutdownGracefully());
            }
            if (futures.isEmpty()) {
                //不需要等到
                if (error == null) {
                    result.complete(ch);
                } else {
                    result.completeExceptionally(error);
                }
            } else {
                //等待线程关闭
                LinkedList<Throwable> throwables = new LinkedList<>();
                if (error != null) {
                    throwables.add(error);
                }
                AtomicInteger counter = new AtomicInteger(futures.size());
                for (Future future : futures) {
                    future.addListener(f -> {
                        if (!f.isSuccess()) {
                            throwables.add(f.cause() == null ? new TransportException(("unknown exception.")) : f.cause());
                        }
                        if (counter.decrementAndGet() == 0) {
                            if (!throwables.isEmpty()) {
                                result.completeExceptionally(throwables.peek());
                            } else {
                                result.complete(ch);
                            }
                        }
                    });
                }
            }
        });
        return result;
    }
}
