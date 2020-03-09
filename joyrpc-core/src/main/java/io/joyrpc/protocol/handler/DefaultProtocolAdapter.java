package io.joyrpc.protocol.handler;

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

import io.joyrpc.exception.ProtocolException;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.AdapterContext;
import io.joyrpc.transport.codec.ProtocolAdapter;

import static io.joyrpc.Plugin.SERVER_PROTOCOL;

/**
 * @date: 2019/4/1
 */
public class DefaultProtocolAdapter implements ProtocolAdapter {

    @Override
    public void adapter(AdapterContext context, ChannelBuffer buffer) {
        if (buffer.readableBytes() < 1) {
            return;
        }

        int readerIndex = buffer.readerIndex();
        // 遍历协议集进行匹配
        for (ServerProtocol protocol : SERVER_PROTOCOL.extensions()) {
            if (protocol.match(buffer)) {
                context.bind(protocol.getCodec(), protocol.buildChain());
                context.getChannel().setAttribute(Channel.PROTOCOL, protocol);
                buffer.readerIndex(readerIndex);
                return;
            } else {
                buffer.readerIndex(readerIndex);
            }
        }
        throw new ProtocolException("No matching protocol found");
    }
}
