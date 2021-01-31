package io.joyrpc.transport.netty4.test.http;

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
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.DeductionContext;
import io.joyrpc.transport.codec.HttpCodec;
import io.joyrpc.transport.codec.ProtocolDeduction;
import io.joyrpc.transport.netty4.mock.MockChannelHandler;
import io.joyrpc.transport.netty4.mock.MockCodec;

/**
 * 简单协议推断
 */
public class SimpleDeduction implements ProtocolDeduction {

    @Override
    public void deduce(final DeductionContext context, final ChannelBuffer buffer) {
        if (isHttp(buffer)) {
            context.bind(new HttpCodec(), new ChannelHandlerChain().addLast(new MockChannelHandler()).addLast(new MockHttpChannelHandler()));
        } else {
            context.bind(new MockCodec(), new ChannelHandlerChain().addLast(new MockChannelHandler()).addLast(new MockHttpChannelHandler()));
        }
    }

    /**
     * 判断是否是http协议
     *
     * @param in 缓冲区
     * @return http协议标识
     */
    protected boolean isHttp(final ChannelBuffer in) {
        byte magic1 = (byte) in.getUnsignedByte(0);
        byte magic2 = (byte) in.getUnsignedByte(1);
        return (magic1 == 'G' && magic2 == 'E') || // GET
                (magic1 == 'P' && magic2 == 'O') || // POST
                (magic1 == 'P' && magic2 == 'U') || // PUT
                (magic1 == 'H' && magic2 == 'E') || // HEAD
                (magic1 == 'O' && magic2 == 'P') || // OPTIONS
                (magic1 == 'P' && magic2 == 'A') || // PATCH
                (magic1 == 'D' && magic2 == 'E') || // DELETE
                (magic1 == 'T' && magic2 == 'R') || // TRACE
                (magic1 == 'C' && magic2 == 'O');   // CONNECT
    }
}
