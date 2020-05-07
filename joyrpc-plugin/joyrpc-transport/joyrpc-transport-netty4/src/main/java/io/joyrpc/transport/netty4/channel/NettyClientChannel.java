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
import io.joyrpc.transport.channel.Channel;
import io.netty.channel.EventLoopGroup;

import java.util.function.Consumer;

/**
 * 客户端Channel
 */
public class NettyClientChannel extends NettyChannel {

    /**
     * IO线程池
     */
    protected EventLoopGroup ioGroup;

    /**
     * 构造函数
     *
     * @param channel channel
     * @param ioGroup 线程池
     */
    public NettyClientChannel(final io.netty.channel.Channel channel, final EventLoopGroup ioGroup) {
        super(channel, false);
        this.ioGroup = ioGroup;
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        super.close(o -> {
            if (consumer != null && ioGroup == null) {
                //不需要等到
                consumer.accept(o.isSuccess() ? new AsyncResult<>(this) : new AsyncResult<>(this, o.getThrowable()));
            } else if (consumer != null) {
                //强制关闭
                ioGroup.shutdownGracefully().addListener(f -> consumer.accept(o));
            } else if (ioGroup != null) {
                ioGroup.shutdownGracefully();
            }
        });
    }
}
