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

import io.joyrpc.event.AsyncResult;
import io.joyrpc.exception.TransportException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ServerChannel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @date: 2019/3/6
 */
public class NettyServerChannel extends NettyChannel implements ServerChannel {

    /**
     * 服务端上下文
     */
    protected Supplier<List<Channel>> supplier;

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
     * @param channel
     * @param bossGroup
     * @param workerGroup
     * @param supplier
     */
    public NettyServerChannel(final io.netty.channel.Channel channel,
                              final EventLoopGroup bossGroup,
                              final EventLoopGroup workerGroup,
                              final Supplier<List<Channel>> supplier) {
        super(channel, true);
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.supplier = supplier;
    }

    @Override
    public List<Channel> getChannels() {
        return supplier.get();
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        super.close(o -> {
            List<Future> futures = new LinkedList<>();
            if (bossGroup != null) {
                futures.add(bossGroup.shutdownGracefully());
            }
            if (workerGroup != null) {
                futures.add(workerGroup.shutdownGracefully());
            }
            if (consumer != null && futures.isEmpty()) {
                //不需要等到
                consumer.accept(o.isSuccess() ? new AsyncResult<>(this) : new AsyncResult<>(this, o.getThrowable()));
            } else if (consumer != null) {
                //等待线程关闭
                LinkedList<Throwable> throwables = new LinkedList<>();
                if (!o.isSuccess()) {
                    throwables.add(o.getThrowable());
                }
                AtomicInteger counter = new AtomicInteger(futures.size());
                for (Future future : futures) {
                    future.addListener(f -> {
                        if (!f.isSuccess()) {
                            throwables.add(f.cause() == null ? new TransportException(("unknown exception.")) : f.cause());
                        }
                        if (counter.decrementAndGet() == 0) {
                            if (!throwables.isEmpty()) {
                                consumer.accept(new AsyncResult<>(this, throwables.peek()));
                            } else {
                                consumer.accept(new AsyncResult<>(this));
                            }
                        }
                    });
                }
            }
        });
    }
}
