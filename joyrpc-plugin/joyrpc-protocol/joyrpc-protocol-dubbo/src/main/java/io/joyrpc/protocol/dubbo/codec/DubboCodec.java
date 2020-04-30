package io.joyrpc.protocol.dubbo.codec;

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

import io.joyrpc.protocol.AbstractCodec;
import io.joyrpc.protocol.Protocol;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.message.Header;

/**
 * Dubbo编解码
 */
public class DubboCodec extends AbstractCodec {
    //刨去MagicCode的2个字节14=16-2
    protected static final short HEADER_LENGTH = 14;
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;
    protected static final int SERIALIZATION_MASK = 0x1f;

    protected static final byte HESSIAN2_SERIALIZATION_ID = 2;
    protected static final byte JAVA_SERIALIZATION_ID = 3;
    protected static final byte COMPACTED_JAVA_SERIALIZATION_ID = 4;
    protected static final byte FASTJSON_SERIALIZATION_ID = 6;
    protected static final byte NATIVE_JAVA_SERIALIZATION_ID = 7;
    protected static final byte KRYO_SERIALIZATION_ID = 8;
    protected static final byte FST_SERIALIZATION_ID = 9;
    protected static final byte NATIVE_HESSIAN_SERIALIZATION_ID = 10;
    protected static final byte PROTOSTUFF_SERIALIZATION_ID = 12;
    protected static final byte AVRO_SERIALIZATION_ID = 11;
    protected static final byte GSON_SERIALIZATION_ID = 16;
    protected static final byte PROTOBUF_JSON_SERIALIZATION_ID = 21;
    protected static final byte PROTOBUF_SERIALIZATION_ID = 22;
    protected static final byte KRYO_SERIALIZATION2_ID = 25;

    /**
     * 构造函数
     *
     * @param protocol 协议
     */
    public DubboCodec(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Header decodeHeader(final ChannelBuffer buffer) {
        MessageHeader header = new MessageHeader();

        header.setHeaderLength(HEADER_LENGTH);
        byte flag = buffer.readByte();
        boolean request = (flag & FLAG_REQUEST) > 0;
        boolean event = (flag & FLAG_EVENT) > 0;
        boolean twoWay = (flag & FLAG_TWOWAY) > 0;
        //应答状态
        byte status = buffer.readByte();
        //4-11
        long requestId = buffer.readLong();
        //12-15
        int len = buffer.readInt();
        //数据长度
        header.setLength(len + HEADER_LENGTH);
        return header;
    }
}
