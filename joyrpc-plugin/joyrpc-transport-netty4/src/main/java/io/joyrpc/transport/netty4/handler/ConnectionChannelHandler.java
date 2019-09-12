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

import io.joyrpc.event.Publisher;
import io.joyrpc.exception.ChannelClosedException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.EnhanceCompletableFuture;
import io.joyrpc.transport.event.ActiveEvent;
import io.joyrpc.transport.event.InactiveEvent;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 连接处理器
 */
public class ConnectionChannelHandler extends ChannelInboundHandlerAdapter {

    protected final static Logger logger = LoggerFactory.getLogger(ConnectionChannelHandler.class);
    /**
     * 通道
     */
    protected Channel channel;
    /**
     * 事件发布器
     */
    protected Publisher<TransportEvent> eventPublisher;

    /**
     * 构造函数
     *
     * @param channel
     * @param eventPublisher
     */
    public ConnectionChannelHandler(Channel channel, Publisher<TransportEvent> eventPublisher) {
        this.channel = channel;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!ctx.channel().isWritable()) {
            logger.error(String.format("Discard request, because client is sending too fast, causing channel is not writable. at %s : %s",
                    Channel.toString(channel), msg.toString()));
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Map<Integer, EnhanceCompletableFuture<Integer, Message>> futures = channel.getFutureManager().close();
        futures.forEach((id, future) ->
                future.completeExceptionally(new ChannelClosedException("channel is inactive, address is " + channel.getRemoteAddress())));
        futures.clear();
        eventPublisher.offer(new InactiveEvent(channel));
        ctx.fireChannelInactive();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        eventPublisher.offer(new ActiveEvent(channel));
        ctx.fireChannelActive();
    }
}
