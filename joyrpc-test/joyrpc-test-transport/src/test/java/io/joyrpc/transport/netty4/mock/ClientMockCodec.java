package io.joyrpc.transport.netty4.mock;

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


import io.joyrpc.exception.CodecException;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.codec.DecodeContext;
import io.joyrpc.transport.codec.EncodeContext;
import io.joyrpc.transport.codec.LengthFieldFrameCodec;

/**
 * @date: 2019/1/28
 */
public class ClientMockCodec implements LengthFieldFrameCodec {

    @Override
    public Object decode(DecodeContext context, ChannelBuffer buffer) throws CodecException {
        MockMessage mockMessage = new MockMessage();
        int len = buffer.readInt();
        int id = buffer.readInt();
        MsgType msgType = MsgType.valueOf(buffer.readByte());
        int readable = buffer.readableBytes();
        int payLoadLen = len - 13;
        byte[] data = new byte[payLoadLen > readable ? readable : payLoadLen];
        buffer.readBytes(data);
        mockMessage.setHeader(new MockMessageHeader(id, msgType.getType()));
        mockMessage.setPayLoad(data);
        return mockMessage;
    }


    @Override
    public LengthFieldFrame getLengthFieldFrame() {
        return new LengthFieldFrame(4, 4, -8, 4);
    }

    @Override
    public void encode(EncodeContext context, ChannelBuffer buffer, Object message) throws CodecException {
        MockMessage mockMessage = (MockMessage) message;
        buffer.writeBytes(new byte[]{'M', 'O', 'C', 'K'});
        buffer.writeInt(mockMessage.getPayLoad().length + 13);
        buffer.writeInt(mockMessage.getHeader().getMsgId());
        buffer.writeByte(mockMessage.getMsgType());
        buffer.writeBytes(mockMessage.getPayLoad());
    }
}
