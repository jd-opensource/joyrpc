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
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.event.ActiveEvent;
import io.joyrpc.transport.event.InactiveEvent;
import io.joyrpc.transport.event.TransportEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接处理器，触发连接和断连事件
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
    protected Publisher<TransportEvent> publisher;

    /**
     * 构造函数
     *
     * @param channel   连接通道
     * @param publisher 事件发布器
     */
    public ConnectionChannelHandler(Channel channel, Publisher<TransportEvent> publisher) {
        this.channel = channel;
        this.publisher = publisher;
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        try {
            channel.getFutureManager().close();
        } finally {
            publisher.offer(new InactiveEvent(channel));
            ctx.fireChannelInactive();
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        publisher.offer(new ActiveEvent(channel));
        ctx.fireChannelActive();
    }
}
