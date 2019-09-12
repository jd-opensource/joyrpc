package io.joyrpc.transport.netty4.http2;

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
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.transport.netty4.handler.NettyChannelContext;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * @date: 2019/4/10
 */
public class SimpleHttp2BizHandler extends ChannelDuplexHandler {

    protected ChannelHandler handler;
    protected Channel channel;

    public SimpleHttp2BizHandler(ChannelHandler handler, Channel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            handler.received(new NettyChannelContext(channel), msg);
        } catch (Exception e) {
            exceptionCaught(ctx, e);
        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        try {
            Object resMsg = handler.wrote(new NettyChannelContext(channel), msg);
            ctx.write(resMsg, promise);
        } catch (Exception e) {
            exceptionCaught(ctx, e);
        }

    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        handler.active(new NettyChannelContext(channel));
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        handler.active(new NettyChannelContext(channel));
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        handler.caught(new NettyChannelContext(channel), cause);
    }
}
