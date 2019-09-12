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
import io.joyrpc.transport.codec.AdapterContext;
import io.joyrpc.transport.codec.HttpCodec;
import io.joyrpc.transport.codec.ProtocolAdapter;
import io.joyrpc.transport.netty4.mock.MockChannelHandler;
import io.joyrpc.transport.netty4.mock.MockCodec;

/**
 * @date: 2019/1/30
 */
public class SimpleAdapter implements ProtocolAdapter {

    @Override
    public void adapter(AdapterContext context, ChannelBuffer buffer) {
        if (isHttp(buffer)) {
            context.bind(new HttpCodec(),
                    new ChannelHandlerChain()
                            .addLast(new MockChannelHandler())
                            .addLast(new MockHttpChannelHandler()));
        } else {
            context.bind(new MockCodec(), new ChannelHandlerChain()
                    .addLast(new MockChannelHandler())
                    .addLast(new MockHttpChannelHandler()));
        }
    }

    private boolean isHttp(ChannelBuffer in) {
        Short magiccode_high = in.getUnsignedByte(0);
        Short magiccode_low = in.getUnsignedByte(1);
        byte magic1 = magiccode_high.byteValue();
        byte magic2 = magiccode_low.byteValue();
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
