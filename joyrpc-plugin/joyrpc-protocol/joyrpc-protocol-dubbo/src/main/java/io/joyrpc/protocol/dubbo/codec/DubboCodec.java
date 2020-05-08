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

import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.CodecException;
import io.joyrpc.protocol.AbstractCodec;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.Protocol;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;
import io.joyrpc.protocol.dubbo.message.DubboMessageHeader;
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

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

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
        DubboMessageHeader header = new DubboMessageHeader();

        byte flag = buffer.readByte();
        boolean request = (flag & FLAG_REQUEST) != 0;
        boolean event = (flag & FLAG_EVENT) != 0;
        header.setTwoWay((flag & FLAG_TWOWAY) > 0);
        //设置消息类型
        if (request) {
            header.setMsgType(!event ? MsgType.BizReq.getType() : MsgType.HbReq.getType());
        } else {
            header.setMsgType(!event ? MsgType.BizResp.getType() : MsgType.HbResp.getType());
        }
        //读取序列化，这里不转换，由DubboProtocol的converter转换
        byte serial = (byte) (flag & SERIALIZATION_MASK);
        if (serial <= 0) {
            throw new CodecException(String.format("Error occurs while decoding. unsupported serial type %d!", serial), ExceptionCode.CODEC_SERIALIZER_EXCEPTION);
        }
        header.setSerialization(serial);
        //应答状态
        header.setStatus(buffer.readByte());
        //4-11
        header.setMsgId(buffer.readLong());
        //12-15
        int len = buffer.readInt();
        //数据长度
        header.setHeaderLength(HEADER_LENGTH);
        header.setLength(len + HEADER_LENGTH);

        return header;
    }

    @Override
    protected Class getPayloadClass(final MsgType type) {
        switch (type) {
            case BizReq:
                return DubboInvocation.class;
            default:
                return type.getPayloadClz();
        }
    }

    @Override
    public LengthFieldFrame getLengthFieldFrame() {
        return new LengthFieldFrame(12, 4, 0, 2);
    }
}
