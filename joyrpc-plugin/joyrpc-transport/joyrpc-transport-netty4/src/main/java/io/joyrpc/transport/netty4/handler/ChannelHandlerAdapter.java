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
import io.joyrpc.transport.channel.ChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;


/**
 * 把Netty的处理器调用转换成连接通道处理器调用
 */
public class ChannelHandlerAdapter extends ChannelDuplexHandler {

    /**
     * 处理器
     */
    protected ChannelHandler handler;
    /**
     * 同道
     */
    protected Channel channel;

    public ChannelHandlerAdapter(ChannelHandler handler, Channel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        //触发业务处理器的接收
        handler.received(new NettyChannelContext(channel), msg);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        //触发业务处理器的写
        Object resMsg = handler.wrote(new NettyChannelContext(channel), msg);
        if (resMsg != null) {
            ctx.writeAndFlush(resMsg, promise);
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        //触发业务处理器的连接
        handler.active(new NettyChannelContext(channel));
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        //触发业务处理器的断连
        handler.inactive(new NettyChannelContext(channel));
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        //触发业务处理器的异常捕获
        handler.caught(new NettyChannelContext(channel), cause);
    }

}
