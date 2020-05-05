package io.joyrpc.transport.netty4.test.serialization;

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

import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.exception.CodecException;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.DecodeContext;
import io.joyrpc.transport.codec.EncodeContext;
import io.joyrpc.transport.netty4.mock.MockMessageHeader;
import io.joyrpc.transport.netty4.mock.MsgType;

import static io.joyrpc.Plugin.SERIALIZATION;

/**
 * @date: 2019/3/22
 */
public class SerializationCodec implements Codec {

    @Override
    public Object decode(DecodeContext context, ChannelBuffer buffer) throws CodecException {
        //读魔术位
        buffer.readBytes(new byte[4]);
        //读长度
        int length = buffer.readInt();
        //读id
        int id = buffer.readInt();
        //读消息类型
        MsgType msgType = MsgType.valueOf(buffer.readByte());
        //读body并反序列化
        Serializer serializer = SERIALIZATION.get("json@fastjson").getSerializer();
        MessageBody body = serializer.deserialize(buffer.inputStream(), MessageBody.class);
        //赋值
        MockBodyMessage mockMessage = new MockBodyMessage();
        mockMessage.setHeader(new MockMessageHeader(id, msgType.getType()));
        mockMessage.setPayLoad(body);
        return mockMessage;
    }

    @Override
    public void encode(EncodeContext context, ChannelBuffer buffer, Object message) throws CodecException {
        MockBodyMessage mockMessage = (MockBodyMessage) message;
        //写魔术位
        buffer.writeBytes(new byte[]{'M', 'O', 'C', 'K'});
        //写长度
        buffer.writeInt(13);
        //写id
        buffer.writeInt((int) mockMessage.getHeader().getMsgId());
        //写消息类型
        buffer.writeByte(mockMessage.getMsgType());
        //写body并序列化
        Serializer serializer = SERIALIZATION.get("json@fastjson").getSerializer();
        serializer.serialize(buffer.outputStream(), mockMessage.getPayLoad());
    }
}
