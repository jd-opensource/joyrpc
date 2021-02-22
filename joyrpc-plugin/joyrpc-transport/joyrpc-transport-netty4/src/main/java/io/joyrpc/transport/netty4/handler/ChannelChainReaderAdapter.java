package io.joyrpc.transport.netty4.handler;

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
import io.joyrpc.transport.channel.ChannelChainReaderContext;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelReader;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;

import static io.joyrpc.transport.netty4.channel.NettyContext.create;

/**
 * 连接处理器，触发连接和断连事件
 */
public class ChannelChainReaderAdapter extends ChannelInboundHandlerAdapter {

    protected final static Logger logger = LoggerFactory.getLogger(ChannelChainReaderAdapter.class);
    /**
     * 处理器
     */
    protected final ChannelReader[] readers;
    /**
     * 同道
     */
    protected final Channel channel;
    /**
     * 线程池
     */
    protected final ThreadPoolExecutor executor;

    public ChannelChainReaderAdapter(final ChannelReader[] readers, final Channel channel, final ThreadPoolExecutor executor) {
        this.readers = readers;
        this.channel = channel;
        this.executor = executor;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ChannelChainReaderContext context = new ChannelChainReaderContext(channel, readers, create(channel, ctx));
        context.fireChannelActive();
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        ChannelChainReaderContext context = new ChannelChainReaderContext(channel, readers, create(channel, ctx));
        context.fireChannelInactive();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        ChannelChainReaderContext context = new ChannelChainReaderContext(channel, readers, create(channel, ctx));
        if (executor != null) {
            try {
                executor.execute(new ReceiveJob(context, msg));
            } catch (Throwable e) {
                //可能抛出RejectedExecutionException
                context.fireExceptionCaught(e);
            }
        } else {
            context.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        ChannelChainReaderContext context = new ChannelChainReaderContext(channel, readers, create(channel, ctx));
        context.fireExceptionCaught(cause);
    }

    /**
     * 收到数据的任务
     */
    protected static class ReceiveJob implements Runnable, Comparable {
        /**
         * 上下文
         */
        protected final ChannelContext context;
        /**
         * 消息
         */
        protected final Object message;

        public ReceiveJob(final ChannelContext context, final Object message) {
            this.context = context;
            this.message = message;
        }

        @Override
        public int compareTo(final Object o) {
            return 0;
        }

        @Override
        public void run() {
            context.fireChannelRead(message);
        }
    }
}
