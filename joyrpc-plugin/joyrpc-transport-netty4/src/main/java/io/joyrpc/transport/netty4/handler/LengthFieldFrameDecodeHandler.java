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

import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @date: 2019/3/26
 */
public class LengthFieldFrameDecodeHandler extends LengthFieldBasedFrameDecoder {
    /**
     * 编解码
     */
    protected Codec codec;
    /**
     * Channel
     */
    protected Channel channel;

    /**
     * 构造函数
     *
     * @param maxFrameLength
     * @param lengthFieldOffset
     * @param lengthFieldLength
     * @param lengthAdjustment
     * @param initialBytesToStrip
     * @param codec
     * @param channel
     */
    public LengthFieldFrameDecodeHandler(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                         int lengthAdjustment, int initialBytesToStrip,
                                         Codec codec, Channel channel) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        this.codec = codec;
        this.channel = channel;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object obj = super.decode(ctx, in);
        if (obj instanceof ByteBuf) {
            ChannelBuffer buf = new NettyChannelBuffer((ByteBuf) obj);
            try {
                return codec.decode(() -> channel, buf);
            } finally {
                if (!buf.isReleased()) {
                    buf.release();
                }
            }
        } else {
            return obj;
        }
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }
}
