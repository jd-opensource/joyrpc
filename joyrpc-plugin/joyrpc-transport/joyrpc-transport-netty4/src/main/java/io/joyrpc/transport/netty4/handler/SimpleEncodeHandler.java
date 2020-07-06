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
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.function.BiFunction;

/**
 * @date: 2019/1/15
 */
public class SimpleEncodeHandler extends MessageToByteEncoder {

    /**
     * 函数
     */
    public static final BiFunction<Codec, Channel, ChannelHandler> FUNCTION = (c, l) -> new SimpleEncodeHandler(c, l);
    /**
     * 编解码
     */
    protected Codec codec;
    /**
     * 同道
     */
    protected Channel channel;

    /**
     * 构造函数
     *
     * @param codec
     * @param channel
     */
    public SimpleEncodeHandler(Codec codec, Channel channel) {
        this.codec = codec;
        this.channel = channel;
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Object msg, final ByteBuf out) throws Exception {
        try {
            codec.encode(() -> channel, new NettyChannelBuffer(out), msg);
        } catch (Throwable throwable) {
            ctx.fireExceptionCaught(throwable);
        }
    }

}
