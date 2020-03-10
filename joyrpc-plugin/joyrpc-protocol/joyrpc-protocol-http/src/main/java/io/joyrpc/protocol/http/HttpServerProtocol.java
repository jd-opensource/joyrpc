package io.joyrpc.protocol.http;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.AbstractProtocol;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.protocol.handler.RequestChannelHandler;
import io.joyrpc.protocol.handler.ResponseChannelHandler;
import io.joyrpc.protocol.http.handler.HttpToJoyHandler;
import io.joyrpc.protocol.http.handler.JoyToHttpHandler;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.HttpCodec;

import static io.joyrpc.protocol.Protocol.HTTP_ORDER;

/**
 * HTTP服务协议
 */
@Extension(value = "http", order = HTTP_ORDER)
public class HttpServerProtocol extends AbstractProtocol implements ServerProtocol {

    @Override
    public boolean match(ChannelBuffer channelBuffer) {
        short highMagicCode = channelBuffer.getUnsignedByte(0);
        short lowMagicCode = channelBuffer.getUnsignedByte(1);
        byte magic1 = (byte) highMagicCode;
        byte magic2 = (byte) lowMagicCode;

        return (magic1 == 'G' && magic2 == 'E') || // GET
                (magic1 == 'P' && magic2 == 'O') || // POST
                (magic1 == 'P' && magic2 == 'U') || // PUT
                (magic1 == 'H' && magic2 == 'E') || // HEAD
                (magic1 == 'O' && magic2 == 'P') || // OPTIONS
                (magic1 == 'P' && magic2 == 'A') || // PATCH
                (magic1 == 'D' && magic2 == 'E') || // DELETE
                (magic1 == 'T' && magic2 == 'R') || // TRACE
                (magic1 == 'C' && magic2 == 'O');
    }

    @Override
    public ChannelHandlerChain buildChain() {
        if (chain == null) {
            chain = new ChannelHandlerChain()
                    .addLast(new HttpToJoyHandler())
                    .addLast(new RequestChannelHandler<>(io.joyrpc.Plugin.MESSAGE_HANDLER_SELECTOR, this::onException))
                    .addLast(new JoyToHttpHandler())
                    .addLast(new ResponseChannelHandler());
        }
        return chain;
    }

    @Override
    protected Codec createCodec() {
        return HttpCodec.INSTANCE;
    }

    @Override
    public byte[] getMagicCode() {
        return new byte[0];
    }

}
