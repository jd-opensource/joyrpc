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
import io.joyrpc.transport.channel.ChannelChainWriterContext;
import io.joyrpc.transport.channel.ChannelWriter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static io.joyrpc.transport.netty4.channel.NettyContext.create;

/**
 * 连接通道写适配器
 */
public class ChannelChainWriterAdapter extends ChannelOutboundHandlerAdapter {

    protected final static Logger logger = LoggerFactory.getLogger(ChannelChainWriterAdapter.class);
    /**
     * 处理器
     */
    protected final ChannelWriter[] writers;
    /**
     * 同道
     */
    protected final Channel channel;
    /**
     * 线程池
     */
    protected final ExecutorService workerPool;

    public ChannelChainWriterAdapter(final ChannelWriter[] writers, final Channel channel) {
        this.writers = writers;
        this.channel = channel;
        this.workerPool = channel.getWorkerPool();
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        ChannelChainWriterContext context = new ChannelChainWriterContext(channel, writers, create(channel, ctx));
        context.wrote(msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        ChannelChainWriterContext context = new ChannelChainWriterContext(channel, writers, create(channel, ctx));
        context.fireExceptionCaught(cause);
    }
}
