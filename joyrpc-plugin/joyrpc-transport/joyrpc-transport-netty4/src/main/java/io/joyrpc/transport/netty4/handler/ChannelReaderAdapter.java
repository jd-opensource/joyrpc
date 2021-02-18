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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.transport.netty4.channel.NettyContext.create;

/**
 * 连接处理器，触发连接和断连事件
 */
public class ChannelReaderAdapter extends ChannelInboundHandlerAdapter {

    protected final static Logger logger = LoggerFactory.getLogger(ChannelReaderAdapter.class);
    /**
     * 处理器
     */
    protected ChannelReader reader;
    /**
     * 同道
     */
    protected Channel channel;

    public ChannelReaderAdapter(ChannelReader reader, Channel channel) {
        this.reader = reader;
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
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        reader.caught(create(channel, ctx), cause);
    }
}
