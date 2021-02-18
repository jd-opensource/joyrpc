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
import io.joyrpc.transport.channel.ChannelReader;
import io.joyrpc.transport.channel.ChannelWriter;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import static io.joyrpc.transport.netty4.channel.NettyContext.create;


/**
 * 把Netty的处理器调用转换成连接通道处理器调用
 */
public class ChannelOperatorAdapter extends ChannelDuplexHandler {

    /**
     * 处理器
     */
    protected ChannelReader reader;
    /**
     * 处理器
     */
    protected ChannelWriter writer;
    /**
     * 同道
     */
    protected Channel channel;

    public ChannelOperatorAdapter(ChannelReader reader, ChannelWriter writer, Channel channel) {
        this.reader = reader;
        this.writer = writer;
        this.channel = channel;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        reader.active(create(channel, ctx));
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        reader.inactive(create(channel, ctx));
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        reader.received(create(channel, ctx), msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        writer.wrote(create(channel, ctx), msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        reader.caught(create(channel, ctx), cause);
    }

}
