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
import io.joyrpc.transport.codec.ProtocolAdapter;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.joyrpc.transport.netty4.codec.ProtocolAdapterContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 协议适配器，用于检测数据协议
 *
 * @date: 2019/2/18
 */
public class ProtocolAdapterDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolAdapterDecoder.class);

    protected ProtocolAdapter adapter;

    protected Channel channel;

    public ProtocolAdapterDecoder(ProtocolAdapter adapter, Channel channel) {
        this.adapter = adapter;
        this.channel = channel;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object obj) throws Exception {
        if (obj instanceof ByteBuf) {
            //判断buf是否可读
            ByteBuf in = (ByteBuf) obj;
            if (!in.isReadable()) {
                logger.warn("Bytebuf is not readable when decode!");
                return;
            }
            //remove adapter
            ctx.pipeline().remove("adapter");
            //保存位置，进行协议判断
            int readerIndex = in.readerIndex();
            adapter.adapter(new ProtocolAdapterContext(channel, ctx.pipeline()), new NettyChannelBuffer(in));
            in.readerIndex(readerIndex);
            //如果第一个handler是sshHandler，此sshHandler本次不read
            ChannelHandlerContext firstContext = ctx.pipeline().firstContext();
            if (firstContext.handler() instanceof SslHandler) {
                firstContext.fireChannelRead(obj);
            } else {
                ctx.pipeline().fireChannelRead(obj);
            }
        } else {
            super.channelRead(ctx, obj);
        }

    }

}
