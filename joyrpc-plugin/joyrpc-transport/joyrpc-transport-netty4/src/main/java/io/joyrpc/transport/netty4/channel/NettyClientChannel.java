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

import io.joyrpc.event.Publisher;
import io.joyrpc.exception.TransportException;
import io.joyrpc.util.thread.ThreadPool;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.event.TransportEvent;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 客户端Channel
 */
public class NettyClientChannel extends NettyChannel {

    /**
     * IO线程池
     */
    protected final EventLoopGroup ioGroup;

    public NettyClientChannel(final String name,
                              final io.netty.channel.Channel channel,
                              final ThreadPool workerPool,
                              final Publisher<TransportEvent> publisher,
                              final int payloadSize,
                              final EventLoopGroup ioGroup) {
        super(name, channel, workerPool, publisher, payloadSize, false);
        this.ioGroup = ioGroup;
    }

    @Override
    public CompletableFuture<Channel> close() {
        CompletableFuture<Channel> future = new CompletableFuture<>();
        super.close().whenComplete((ch, error) -> {
            try {
                ioGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).addListener(f -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else if (!f.isSuccess()) {
                        future.completeExceptionally(f.cause() == null ? new TransportException(("unknown exception.")) : f.cause());
                    } else {
                        future.complete(ch);
                    }
                });
            } catch (Throwable e) {
                future.completeExceptionally(error == null ? e : error);
            }
        });
        return future;
    }
}
