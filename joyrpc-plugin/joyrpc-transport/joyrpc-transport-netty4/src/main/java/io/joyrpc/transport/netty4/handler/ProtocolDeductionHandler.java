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
import io.joyrpc.transport.codec.ProtocolDeduction;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.joyrpc.transport.netty4.codec.ProtocolDeductionContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.transport.codec.ProtocolDeduction.PROTOCOL_DEDUCTION_HANDLER;

/**
 * 协议推断，用于检测数据协议
 */
public class ProtocolDeductionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolDeductionHandler.class);
    /**
     * 协议推断
     */
    protected final ProtocolDeduction deduction;
    /**
     * 连接通道
     */
    protected final Channel channel;

    public ProtocolDeductionHandler(ProtocolDeduction deduction, Channel channel) {
        this.deduction = deduction;
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
            //第一条数据，进行协议判断，后续删除协议推断处理器
            ctx.pipeline().remove(PROTOCOL_DEDUCTION_HANDLER);
            //协议判断
            deduction.deduce(new ProtocolDeductionContext(channel, ctx.pipeline()), new NettyChannelBuffer(in));
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
