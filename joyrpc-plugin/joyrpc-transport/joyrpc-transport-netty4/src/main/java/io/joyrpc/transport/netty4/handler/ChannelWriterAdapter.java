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
import io.joyrpc.transport.channel.ChannelWriter;
import io.joyrpc.transport.netty4.channel.NettyContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接通道写适配器
 */
public class ChannelWriterAdapter extends ChannelOutboundHandlerAdapter {

    protected final static Logger logger = LoggerFactory.getLogger(ChannelWriterAdapter.class);
    /**
     * 处理器
     */
    protected ChannelWriter writer;
    /**
     * 同道
     */
    protected Channel channel;

    public ChannelWriterAdapter(ChannelWriter writer, Channel channel) {
        this.writer = writer;
        this.channel = channel;
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        writer.wrote(NettyContext.create(channel, ctx), msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        writer.caught(NettyContext.create(channel, ctx), cause);
    }
}
